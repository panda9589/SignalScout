package com.scout.domain.repository;

import com.scout.domain.entity.DocumentChunk;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    long countByDocumentId(Long documentId);

    void deleteByDocumentId(Long documentId);

    @Query("""
            select chunk
            from DocumentChunk chunk
            join fetch chunk.document document
            left join fetch document.company company
            where lower(chunk.chunkText) like lower(concat('%', :query, '%'))
            order by chunk.createdAt desc
            """)
    List<DocumentChunk> searchChunkText(@Param("query") String query, Pageable pageable);
}
