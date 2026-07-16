package com.exp.medic.cics.dto.historial.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record MedicalHistoryDTO(

        @NotNull @Valid
        ConditionDetailDTO hasChronicDisease,

        @NotNull @Valid
        ConditionDetailDTO hasHadMajorSurgeries,

        @NotNull @Valid
        MedicationDTO takesMedication,

        @NotNull @Valid
        ConditionDetailDTO hasAllergies
) {
}
