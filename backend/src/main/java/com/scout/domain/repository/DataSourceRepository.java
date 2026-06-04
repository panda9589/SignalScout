package com.scout.domain.repository;

import com.scout.domain.entity.DataSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DataSourceRepository extends JpaRepository<DataSource, Long> {
    Optional<DataSource> findBySourceType(String sourceType);
}
