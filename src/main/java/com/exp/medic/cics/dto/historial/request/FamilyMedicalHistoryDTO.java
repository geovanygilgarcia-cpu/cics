package com.exp.medic.cics.dto.historial.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record FamilyMedicalHistoryDTO(

        @NotNull(message = "Debe indicar si hay antecedentes familiares")
        Boolean hasImmediateFamilyHistory,

        @NotNull
        Boolean heartDisease,

        @NotNull
        Boolean highBloodPressure,

        @NotNull
        Boolean diabetes,

        @NotNull @Valid
        ConditionDetailDTO cancer,

        @NotNull @Valid
        ConditionDetailDTO other
) {
}
