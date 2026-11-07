package com.fastship.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// nhan vien FastShip duyet yeu cau; de trong thi dung gia tri mac dinh
public record ApproveRequest(
        @PositiveOrZero BigDecimal shippingFee,
        @PositiveOrZero @Max(30) Integer estimatedDays,
        @Size(max = 150) String shipperName,
        @Size(max = 20) String shipperPhone) {
}
