package com.scout.application.service;

import com.scout.domain.entity.Company;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyMatchingServiceTest {

    @Test
    void scoresTickerMatchHighest() {
        Company nvidia = Company.builder()
                .ticker("NVDA")
                .name("NVIDIA Corporation")
                .build();

        int score = CompanyMatchingService.score(
                nvidia,
                CompanyMatchingService.normalize("NVDA announced stronger data center demand."));

        assertThat(score).isGreaterThanOrEqualTo(100);
    }

    @Test
    void scoresCompanyNameAliasMatch() {
        Company microsoft = Company.builder()
                .ticker("MSFT")
                .name("Microsoft Corporation")
                .build();

        int score = CompanyMatchingService.score(
                microsoft,
                CompanyMatchingService.normalize("Microsoft Azure demand improved."));

        assertThat(score).isGreaterThanOrEqualTo(80);
    }

    @Test
    void doesNotMatchTickerInsideWord() {
        Company amd = Company.builder()
                .ticker("AMD")
                .name("Advanced Micro Devices Inc.")
                .build();

        int score = CompanyMatchingService.score(
                amd,
                CompanyMatchingService.normalize("Demand for chips improved."));

        assertThat(score).isZero();
    }
}
