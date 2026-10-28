package com.example.sana.Service.Business;

import com.example.sana.DTO.Business.BusinessToolRequestDTO;
import com.example.sana.DTO.AIDTOs.AIRequestDTO;
import com.example.sana.DTO.AIDTOs.AIRunResponseDTO;
import com.example.sana.Enums.AITask;
import com.example.sana.Service.AIServices.AIRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class BusinessService {
    private final AIRequestService requests;
    private final BusinessAnalysisService analysis;

    public AIRunResponseDTO executeTool(BusinessToolRequestDTO request) {
        if (request == null || request.projectId() == null || request.projectId() <= 0 || request.toolType() == null)
            throw new IllegalArgumentException("Project and business tool are required");
        var ai = new AIRequestDTO(); ai.setTask(AITask.ADVISE);
        ai.setPrompt("Run business analysis: " + request.toolType().name()); ai.setOutputId(request.outputId()); ai.setBaseRevision(request.baseRevision());
        return requests.startBusiness(request.projectId(), ai, Map.of("businessToolType", request.toolType().name(),
                "businessInput", analysis.normalizeInput(request.inputJson())));
    }
}
