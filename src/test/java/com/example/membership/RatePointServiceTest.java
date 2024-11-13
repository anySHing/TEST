package com.example.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RatePointServiceTest {

    @InjectMocks
    private RatePointService ratePointService;

    @Test
    @DisplayName("만원쓰면 백원 적립해줌")
    void paid10kWon() {
        // given
        final int price = 10000;

        // when
        final int result = ratePointService.calculateAmount(price);

        // then
        assertThat(result).isEqualTo(100);
    }

    @Test
    @DisplayName("이만원 쓰면 이백원 적립")
    void paid20kWon() {
        // given
        final int price = 20000;

        // when
        final int result = ratePointService.calculateAmount(price);

        // then
        assertThat(result).isEqualTo(200);
    }

    @Test
    @DisplayName("삼만원 쓰면 삼만원 적립")
    void paid30kWon() {
        // given
        final int price = 30000;

        // when
        final int result = ratePointService.calculateAmount(price);

        // then
        assertThat(result).isEqualTo(300);
    }
}
