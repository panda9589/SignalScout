package com.scout.application.service;

import com.scout.domain.entity.Company;
import com.scout.domain.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CompanyMatchingService {

    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public Optional<Company> matchWatchlistCompany(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        String normalized = normalize(text);
        return companyRepository.findAll().stream()
                .map(company -> new MatchCandidate(company, score(company, normalized)))
                .filter(candidate -> candidate.score() > 0)
                .max(Comparator.comparingInt(MatchCandidate::score))
                .map(MatchCandidate::company);
    }

    static int score(Company company, String normalizedText) {
        int score = 0;
        String ticker = company.getTicker() == null ? "" : company.getTicker().trim().toUpperCase(Locale.ROOT);
        if (!ticker.isBlank() && Pattern.compile("\\b" + Pattern.quote(ticker) + "\\b")
                .matcher(normalizedText)
                .find()) {
            score += 100;
        }

        for (String alias : aliases(company)) {
            if (!alias.isBlank() && normalizedText.contains(normalize(alias))) {
                score += alias.length() >= 8 ? 80 : 40;
            }
        }

        return score;
    }

    static List<String> aliases(Company company) {
        String name = company.getName() == null ? "" : company.getName();
        String simplified = name
                .replaceAll("(?i)\\b(incorporated|inc|corporation|corp|company|co|ltd|limited|plc|class a)\\b\\.?"," ")
                .replaceAll("\\s+", " ")
                .trim();

        if (simplified.equalsIgnoreCase(name.trim())) {
            return List.of(name.trim());
        }
        return List.of(name.trim(), simplified);
    }

    static String normalize(String value) {
        return value
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record MatchCandidate(Company company, int score) {
    }
}
