package com.madebyluoj.framegrabber.service;

/**
 * @author luoJ
 * @date 2023/12/29 10:17
 */
public interface IGrabberService {

    /**
     * 根据摄像头id开启视频流
     * @param cameraId
     * @param rtsp
     */
    void start(Integer cameraId,String rtsp);

    /**
     * 根据摄像机id关闭视频流
     * @param cameraId
     */
    void close(Integer cameraId);
}
