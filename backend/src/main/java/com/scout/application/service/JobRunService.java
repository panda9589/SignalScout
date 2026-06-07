package com.scout.application.service;

import com.scout.application.dto.JobRunDto;
import com.scout.domain.entity.JobRun;
import com.scout.domain.repository.JobRunRepository;
import com.scout.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobRunService {

    private final JobRunRepository jobRunRepository;

    @Transactional
    public JobRun start(String jobName, int documentsFound) {
        JobRun jobRun = JobRun.builder()
                .jobName(jobName)
                .status("running")
                .documentsFound(documentsFound)
                .documentsProcessed(0)
                .aiCalls(0)
                .build();
        return jobRunRepository.save(jobRun);
    }

    @Transactional
    public JobRun complete(JobRun jobRun, int documentsProcessed) {
        jobRun.setStatus("success");
        jobRun.setFinishedAt(LocalDateTime.now());
        jobRun.setDocumentsProcessed(documentsProcessed);
        return jobRunRepository.save(jobRun);
    }

    @Transactional
    public JobRun complete(JobRun jobRun, int documentsProcessed, int aiCalls) {
        jobRun.setStatus("success");
        jobRun.setFinishedAt(LocalDateTime.now());
        jobRun.setDocumentsProcessed(documentsProcessed);
        jobRun.setAiCalls(aiCalls);
        return jobRunRepository.save(jobRun);
    }

    @Transactional
    public JobRun fail(JobRun jobRun, Exception exception) {
        jobRun.setStatus("failed");
        jobRun.setFinishedAt(LocalDateTime.now());
        jobRun.setErrorMessage(exception.getMessage());
        return jobRunRepository.save(jobRun);
    }

    @Transactional(readOnly = true)
    public List<JobRunDto> recent(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return jobRunRepository.findAllByOrderByStartedAtDesc(PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobRunDto get(Long id) {
        return jobRunRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + id));
    }

    private JobRunDto toDto(JobRun jobRun) {
        return JobRunDto.builder()
                .id(jobRun.getId())
                .jobName(jobRun.getJobName())
                .startedAt(jobRun.getStartedAt())
                .finishedAt(jobRun.getFinishedAt())
                .status(jobRun.getStatus())
                .errorMessage(jobRun.getErrorMessage())
                .documentsFound(jobRun.getDocumentsFound())
                .documentsProcessed(jobRun.getDocumentsProcessed())
                .aiCalls(jobRun.getAiCalls())
                .estimatedCostUsd(jobRun.getEstimatedCostUsd())
                .build();
    }
}
