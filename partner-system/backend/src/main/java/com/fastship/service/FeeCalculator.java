package com.fastship.service;

import java.math.BigDecimal;

// bieu phi demo: phi goi + 5.000d moi 500g vuot 1kg
public final class FeeCalculator {

    private FeeCalculator() {
    }

    public static String normalizeService(String serviceType) {
        return serviceType == null || serviceType.isBlank() ? "standard" : serviceType.toLowerCase();
    }

    public static String serviceName(String serviceType) {
        return switch (normalizeService(serviceType)) {
            case "express" -> "FastShip Hỏa Tốc";
            case "fast" -> "FastShip Giao Nhanh";
            default -> "FastShip Tiêu Chuẩn";
        };
    }

    public static BigDecimal fee(String serviceType, Integer weightGram) {
        int base = switch (normalizeService(serviceType)) {
            case "express" -> 60000;
            case "fast" -> 45000;
            default -> 30000;
        };
        int weight = weightGram == null ? 500 : weightGram;
        int extra = weight > 1000 ? (int) Math.ceil((weight - 1000) / 500.0) * 5000 : 0;
        return BigDecimal.valueOf(base + extra);
    }

    public static int estimatedDays(String serviceType) {
        return switch (normalizeService(serviceType)) {
            case "express" -> 1;
            case "fast" -> 2;
            default -> 3;
        };
    }
}
