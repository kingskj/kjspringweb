package com.kjweb.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GreetingBatchScheduler {

    private final JobOperator jobOperator;
    @Qualifier("morningGreetingJob")
    private final Job morningGreetingJob;
    @Qualifier("afternoonGreetingJob")
    private final Job afternoonGreetingJob;
    @Qualifier("cleanupGreetingJob")
    private final Job cleanupGreetingJob;

    @Scheduled(cron = "0 20 19 * * *", zone = "Asia/Seoul")
    public void runMorningGreetingJob() {
        launch(morningGreetingJob, "morning-19-20");
    }

    @Scheduled(cron = "0 20 19 * * *", zone = "Asia/Seoul")
    public void runAfternoonGreetingJob() {
        launch(afternoonGreetingJob, "afternoon-19-20");
    }

    @Scheduled(cron = "0 22 19 * * *", zone = "Asia/Seoul")
    public void runCleanupGreetingJob() {
        launch(cleanupGreetingJob, "cleanup-19-22");
    }

    private void launch(Job job, String trigger) {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("trigger", trigger)
                .addLong("requestedAt", System.currentTimeMillis())
                .toJobParameters();
        try {
            jobOperator.start(job, jobParameters);
            log.info("Batch job launched. job={}, trigger={}", job.getName(), trigger);
        } catch (Exception e) {
            log.error("Batch job launch failed. job={}, trigger={}", job.getName(), trigger, e);
        }
    }
}
