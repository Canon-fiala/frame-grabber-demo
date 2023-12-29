package com.madebyluoj.framegrabber.service;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.madebyluoj.framegrabber.dao.SysCameraMapper;
import com.madebyluoj.framegrabber.entity.SysCamera;
import com.madebyluoj.framegrabber.util.FileUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * @author luoJ
 * @date 2023/12/28 9:20
 */
@Service
public class GrabberServiceImpl extends ServiceImpl<SysCameraMapper, SysCamera> implements IGrabberService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrabberServiceImpl.class);

    private final Map<Integer, FFmpegFrameGrabber> CAMERA_ID_GRABBER_MAP = new HashMap<>();

    @Value("${recognition.fps}")
    private Integer recognitionFps;

    @Resource
    private SysCameraMapper sysCameraMapper;

    @Resource
    private Executor executor;
    @Resource
    private FileUtil fileUtil;

    /**
     * 项目启动时初始化视频流播放和抽帧
     */
    @PostConstruct
    public void init() {
        avutil.av_log_set_level(avutil.AV_LOG_PANIC);

        // 获取摄像头
        List<SysCamera> sysCameras = sysCameraMapper.selectList(Wrappers.lambdaQuery(SysCamera.class)
                .select(SysCamera::getId, SysCamera::getRtsp)
                .eq(SysCamera::getDelFlag,0));
        for (SysCamera sysCamera : sysCameras) {
            FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(sysCamera.getRtsp());
            frameGrabber.setOption("rtsp_transport", "tcp");
            CAMERA_ID_GRABBER_MAP.put(sysCamera.getId(), frameGrabber);
            //对每一个摄像头开启一个子线程
            executor.execute(() -> initGrabber(sysCamera.getId(), frameGrabber));
        }
    }

    private void initGrabber(Integer cameraId, FFmpegFrameGrabber grabber) {
        try {
            //开始抽帧
            startGrabber(cameraId, grabber);

            double frameRate = grabber.getFrameRate();
            //一秒内的抽帧间隔 （视频流如果是25帧，这里为25/5 ，按照一秒抽五次进行抽取）
            int interval = (int) (frameRate / recognitionFps);
            long frameCount = 0;


            Frame frame;
            Java2DFrameConverter java2DFrameConverter = new Java2DFrameConverter();
            while ((frame = grabber.grabImage()) != null) {
                if (frameCount % interval == 0) {
                    try {
                        BufferedImage bufferedImage = java2DFrameConverter.getBufferedImage(frame);
                        frame.close();
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        ImageIO.write(bufferedImage, "jpg", outputStream);
                        byte[] bytes = outputStream.toByteArray();
                        //输出抽帧图片
                        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
                        fileUtil.upload(inputStream,".jpg");
                    } catch (Throwable e) {
                        LOGGER.error("Grabber error", e);
                    }
                }
                frameCount++;
            }
        } catch (Throwable e) {
            LOGGER.error("Grabber error, cameraId: " + cameraId, e);
        } finally {
            try {
                grabber.stop();
                LOGGER.info("Grabber stopped, cameraId: " + cameraId);
            } catch (Exception e) {
                LOGGER.error("Grabber stop error, cameraId: " + cameraId, e);
            }

            grabber = CAMERA_ID_GRABBER_MAP.get(cameraId);
            if (grabber != null) {
                initGrabber(cameraId, grabber);
            }
        }
    }

    private void startGrabber(Integer cameraId, FFmpegFrameGrabber grabber) {
        try {
            grabber.start();
            LOGGER.info("Grabber start success, cameraId: " + cameraId);
        } catch (Exception e) {
            LOGGER.error("Grabber start error, cameraId: {}, err: {}", cameraId, e.getMessage());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                LOGGER.error("Grabber start error, cameraId: " + cameraId, ie);
            }
            startGrabber(cameraId, grabber);
        }
    }

    /**
     * 停止视频流抽帧
     */
    private void stopGrabber(Integer cameraId, FFmpegFrameGrabber grabber) {
        try {
            grabber.stop();
            LOGGER.info("Grabber stop success, cameraId: " + cameraId);
        } catch (Exception e) {
            LOGGER.error("Grabber stop error, cameraId: {}, err: {}", cameraId, e.getMessage());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                LOGGER.error("Grabber stop error, cameraId: " + cameraId, ie);
            }
            stopGrabber(cameraId, grabber);
        }
        try {
            grabber.release();
            LOGGER.info("Grabber release success, cameraId: " + cameraId);
        } catch (Exception e) {
            LOGGER.error("Grabber release error, cameraId: {}, err: {}", cameraId, e.getMessage());
        }
    }

    /**
     * 开启视频流的播放
     */
    @Override
    public void start(Integer cameraId, String rtsp) {
        boolean containsKey = CAMERA_ID_GRABBER_MAP.containsKey(cameraId);
        if (containsKey) {
            close(cameraId);
        }

        FFmpegFrameGrabber newGrabber = new FFmpegFrameGrabber(rtsp);
        newGrabber.setOption("rtsp_transport", "tcp");
        newGrabber.setOption("timeout", "3000");
        CAMERA_ID_GRABBER_MAP.put(cameraId, newGrabber);
        executor.execute(() -> initGrabber(cameraId, newGrabber));
    }

    /**
     * 关闭视频流的播放
     */
    @Override
    public void close(Integer cameraId) {
        FFmpegFrameGrabber grabber = CAMERA_ID_GRABBER_MAP.get(cameraId);
        stopGrabber(cameraId, grabber);
        CAMERA_ID_GRABBER_MAP.remove(cameraId);
    }
}
