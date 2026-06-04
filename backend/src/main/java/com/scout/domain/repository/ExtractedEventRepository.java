package com.scout.domain.repository;

import com.scout.domain.entity.ExtractedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExtractedEventRepository extends JpaRepository<ExtractedEvent, Long> {
    List<ExtractedEvent> findByCompanyId(Long companyId, Sort sort);
    List<ExtractedEvent> findByDocumentId(Long documentId);
}
