package com.kjweb.batch.config;

import com.kjweb.batch.service.GreetingBatchService;
import com.kjweb.domain.entity.Board;
import com.kjweb.domain.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class GreetingBatchConfig {

    private final JobRepository jobRepository;
    @Qualifier("transactionManager")
    private final PlatformTransactionManager transactionManager;
    private final GreetingBatchService greetingBatchService;
    private final BoardRepository boardRepository;

    @Bean
    public Job morningGreetingJob() {
        return new JobBuilder("morningGreetingJob", jobRepository)
                .start(morningGreetingStep())
                .build();
    }

    @Bean
    public Step morningGreetingStep() {
        return new StepBuilder("morningGreetingStep", jobRepository)
                .tasklet(morningGreetingTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet morningGreetingTasklet() {
        return (contribution, chunkContext) -> {
            greetingBatchService.createMorningGreeting();
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Job afternoonGreetingJob() {
        return new JobBuilder("afternoonGreetingJob", jobRepository)
                .start(afternoonGreetingStep())
                .build();
    }

    @Bean
    public Step afternoonGreetingStep() {
        return new StepBuilder("afternoonGreetingStep", jobRepository)
                .tasklet(afternoonGreetingTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet afternoonGreetingTasklet() {
        return (contribution, chunkContext) -> {
            greetingBatchService.createAfternoonGreeting();
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Job cleanupGreetingJob() {
        return new JobBuilder("cleanupGreetingJob", jobRepository)
                .start(cleanupGreetingStep())
                .build();
    }

    @Bean
    public Step cleanupGreetingStep() {
        return new StepBuilder("cleanupGreetingStep", jobRepository)
                .tasklet(cleanupGreetingTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet cleanupGreetingTasklet() {
        return (contribution, chunkContext) -> {
            long deletedCount = boardRepository.deleteByBoardType(Board.BoardType.GREETING);
            log.info("Deleted greeting boards count={}", deletedCount);

            int dayOfMonth = LocalDate.now().getDayOfMonth();
            if (dayOfMonth % 2 == 0) {
                throw new RuntimeException("Forced 500 error: even day-of-month detected after cleanup");
            }
            return RepeatStatus.FINISHED;
        };
    }
}
