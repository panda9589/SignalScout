package com.scout.presentation.controller;

import com.scout.application.dto.InvestmentThesisDto;
import com.scout.application.dto.UpsertInvestmentThesisRequest;
import com.scout.application.service.PortfolioService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ThesisControllerTest {

    private final PortfolioService portfolioService = mock(PortfolioService.class);
    private final ThesisController controller = new ThesisController(portfolioService);

    @Test
    void updateThesisReturnsUpdatedThesis() {
        UpsertInvestmentThesisRequest request = new UpsertInvestmentThesisRequest();
        request.setCompanyId(3L);
        request.setThesisText("AI infrastructure demand thesis");
        request.setBuyReason("Strong demand");
        request.setExpectedTimeHorizonMonths(18);
        request.setStatus("active");
        InvestmentThesisDto thesis = InvestmentThesisDto.builder()
                .id(11L)
                .companyId(3L)
                .ticker("NVDA")
                .companyName("NVIDIA Corporation")
                .thesisText("AI infrastructure demand thesis")
                .buyReason("Strong demand")
                .expectedTimeHorizonMonths(18)
                .status("active")
                .build();
        when(portfolioService.updateThesis(11L, request)).thenReturn(thesis);

        var response = controller.updateThesis(11L, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(thesis);
    }

    @Test
    void deleteThesisReturnsNoContent() {
        var response = controller.deleteThesis(11L);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(portfolioService).deleteThesis(11L);
    }
}
