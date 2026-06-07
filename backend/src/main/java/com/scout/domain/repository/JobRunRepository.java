package com.scout.domain.repository;

import com.scout.domain.entity.JobRun;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRunRepository extends JpaRepository<JobRun, Long> {
    List<JobRun> findAllByOrderByStartedAtDesc(Pageable pageable);
}
