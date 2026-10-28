
package com.example.sana.Controller;

import java.util.List;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.sana.Service.ExternalAPI.WorldBankApiService;
import com.example.sana.DTO.WorldBankIndicatorDTO;

@RestController
@RequestMapping("/api/v1/external/world-bank")
@AllArgsConstructor
public class WorldBankController {

    private final WorldBankApiService worldBankApiService;


    @GetMapping("/saudi-indicators")
    public List<WorldBankIndicatorDTO> getSaudiIndicators() {
        return worldBankApiService.getSaudiEconomicIndicators();
    }
}
