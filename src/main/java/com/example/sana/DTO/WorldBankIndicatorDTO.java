package com.example.sana.DTO;

public record WorldBankIndicatorDTO(
        String indicatorCode,
        String indicatorName,
        String countryCode,
        String countryName,
        String year,
        double value,
        String unit
) {}