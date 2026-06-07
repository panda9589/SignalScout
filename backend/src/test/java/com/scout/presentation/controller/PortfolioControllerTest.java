package com.scout.presentation.controller;

import com.scout.application.dto.CreatePortfolioAccountRequest;
import com.scout.application.dto.PortfolioAccountDto;
import com.scout.application.dto.PortfolioHoldingDto;
import com.scout.application.dto.UpsertPortfolioHoldingRequest;
import com.scout.application.service.PortfolioService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PortfolioControllerTest {

    private final PortfolioService portfolioService = mock(PortfolioService.class);
    private final PortfolioController controller = new PortfolioController(portfolioService);

    @Test
    void updateAccountReturnsUpdatedAccount() {
        CreatePortfolioAccountRequest request = new CreatePortfolioAccountRequest();
        request.setAccountName("TFSA");
        request.setAccountType("TFSA");
        request.setBaseCurrency("CAD");
        PortfolioAccountDto account = PortfolioAccountDto.builder()
                .id(7L)
                .accountName("TFSA")
                .accountType("TFSA")
                .baseCurrency("CAD")
                .build();
        when(portfolioService.updateAccount(7L, request)).thenReturn(account);

        var response = controller.updateAccount(7L, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(account);
    }

    @Test
    void deleteAccountReturnsNoContent() {
        var response = controller.deleteAccount(7L);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(portfolioService).deleteAccount(7L);
    }

    @Test
    void updateHoldingReturnsUpdatedHolding() {
        UpsertPortfolioHoldingRequest request = new UpsertPortfolioHoldingRequest();
        request.setAccountId(1L);
        request.setSymbol("NVDA");
        request.setMarketValueCad(BigDecimal.valueOf(750));
        PortfolioHoldingDto holding = PortfolioHoldingDto.builder()
                .id(9L)
                .accountId(1L)
                .symbol("NVDA")
                .marketValueCad(BigDecimal.valueOf(750))
                .build();
        when(portfolioService.updateHolding(9L, request)).thenReturn(holding);

        var response = controller.updateHolding(9L, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(holding);
    }

    @Test
    void deleteHoldingReturnsNoContent() {
        var response = controller.deleteHolding(9L);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(portfolioService).deleteHolding(9L);
    }
}
