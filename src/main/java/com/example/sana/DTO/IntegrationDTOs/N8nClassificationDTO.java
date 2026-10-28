package com.example.sana.DTO.IntegrationDTOs;

// The answer of the n8n classify workflow (<webhook URL>/classify)
public record N8nClassificationDTO(String category, String diagramType, String projectName, String reason) {}
