package com.exp.medic.cics.dto.historial.request;

import jakarta.validation.constraints.NotBlank;

public record ReasonForVisitDTO(

        @NotBlank(message = "Debe describir los síntomas")
        String symptoms,

        @NotBlank(message = "Debe indicar la duración de los síntomas")
        String symptomDuration,

        String previousTreatment
) {
}

