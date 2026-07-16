package com.exp.medic.cics.dto.receta.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record RecetaDTO(
        String folio,

        @NotBlank(message = "El nombre del paciente es obligatorio")
        String paciente,

        String edad,

        @Valid
        FechaDTO fecha,

        @Valid
        SignosVitalesDTO signosVitales,

        String idx,

        String diagnosticoTratamiento,

        String proximaCita,

        String firma
) {}
