package com.groom.e_commerce.user.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class ResSalesStatDtoV1 {

    private LocalDate date;
    private BigDecimal totalAmount;

    public static ResSalesStatDtoV1 of(LocalDate date, BigDecimal totalAmount) {
        return ResSalesStatDtoV1.builder()
                .date(date)
                .totalAmount(totalAmount)
                .build();
    }
}
