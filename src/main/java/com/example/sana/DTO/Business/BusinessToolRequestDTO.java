package com.example.sana.DTO.Business;

import com.example.sana.Enums.BusinessToolType;
import jakarta.validation.constraints.*;

public record BusinessToolRequestDTO(@NotNull @Positive Integer projectId, @NotNull BusinessToolType toolType,
                                     @Size(max = 20000) String inputJson, @NotNull @Positive Integer outputId,
                                     @PositiveOrZero Integer baseRevision) {
    public BusinessToolRequestDTO(Integer projectId, BusinessToolType toolType, String inputJson) {
        this(projectId, toolType, inputJson, null, null);
    }
}