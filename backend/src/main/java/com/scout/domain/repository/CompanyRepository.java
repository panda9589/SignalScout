package com.scout.domain.repository;

import com.scout.domain.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByTicker(String ticker);
    Optional<Company> findByCik(String cik);
}
