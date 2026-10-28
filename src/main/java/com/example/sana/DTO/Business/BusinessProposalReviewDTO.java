package com.example.sana.DTO.Business;

import com.example.sana.Enums.ProposalStatus;
import jakarta.validation.constraints.NotNull;

public record BusinessProposalReviewDTO(

        @NotNull
        ProposalStatus status
) {
}