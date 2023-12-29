package com.madebyluoj.framegrabber.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * @author luoJ
 * @date 2023/12/29 11:24
 */
@Component
public class FileUtil {

    @Value("${spring.servlet.multipart.location}")
    private String path;

    public String upload(InputStream inputStream, String filename) {
        try {
            var newFilename = UUID.randomUUID() + getFileExtension(filename);
            var directory = new File(path);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            var filePath = Paths.get(path, newFilename);
            var bufferSize = 1024; // 设置缓冲区大小
            var buffer = new byte[bufferSize];
            var outputStream = new FileOutputStream(filePath.toFile());
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            outputStream.close();
            return filePath.toAbsolutePath().toString().replaceAll("\\\\", "/");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getFileExtension(String filename) {
        var lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }
}
