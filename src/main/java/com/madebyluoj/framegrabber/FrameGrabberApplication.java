package com.madebyluoj.framegrabber;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FrameGrabberApplication {

    public static void main(String[] args) {
        SpringApplication.run(FrameGrabberApplication.class, args);
    }

}
