package com.scout.presentation.controller;

import com.scout.application.dto.JobRunDto;
import com.scout.application.service.JobRunService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobRunService jobRunService;

    @GetMapping
    public ResponseEntity<List<JobRunDto>> recentJobs(
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return ResponseEntity.ok(jobRunService.recent(limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobRunDto> getJob(@PathVariable Long id) {
        return ResponseEntity.ok(jobRunService.get(id));
    }
}
