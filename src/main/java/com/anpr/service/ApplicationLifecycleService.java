package com.anpr.service;

import com.anpr.AnprAccessControlApplication;
import com.anpr.dto.ActionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationLifecycleService {

    private final ConfigurableApplicationContext applicationContext;
    private final ApplicationArguments applicationArguments;

    public ActionResponse restartApplication() {
        Thread restartThread = new Thread(() -> {
            try {
                Thread.sleep(1500);
                log.warn("Restarting application using runtime settings...");
                SpringApplication.exit(applicationContext, () -> 0);
                SpringApplication.run(AnprAccessControlApplication.class, applicationArguments.getSourceArgs());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.error("Application restart interrupted", ex);
            } catch (Exception ex) {
                log.error("Application restart failed", ex);
            }
        });
        restartThread.setName("anpr-app-restart-thread");
        restartThread.setDaemon(false);
        restartThread.start();

        return ActionResponse.builder()
                .success(true)
                .message("Restart requested. Wait 10-20 seconds and log in again.")
                .build();
    }
}
