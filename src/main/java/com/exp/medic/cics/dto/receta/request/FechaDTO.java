package com.exp.medic.cics.dto.receta.request;

import jakarta.validation.constraints.Pattern;

public record FechaDTO(
        @Pattern(regexp = "\\d{1,2}", message = "Día inválido")
        String dia,

        @Pattern(regexp = "\\d{1,2}", message = "Mes inválido")
        String mes,

        @Pattern(regexp = "\\d{4}", message = "Año inválido")
        String anio
) {}
