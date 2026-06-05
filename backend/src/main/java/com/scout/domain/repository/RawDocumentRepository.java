package com.scout.domain.repository;

import com.scout.domain.entity.RawDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RawDocumentRepository extends JpaRepository<RawDocument, Long> {
    Optional<RawDocument> findByContentHash(String contentHash);
    boolean existsBySourceTypeAndExternalId(String sourceType, String externalId);
    Optional<RawDocument> findBySourceTypeAndExternalId(String sourceType, String externalId);
}
