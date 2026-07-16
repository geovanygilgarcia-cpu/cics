package com.exp.medic.cics.dto.historial.request;

import jakarta.validation.constraints.NotNull;

/**
 * DTO reutilizable para preguntas tipo Sí/No que incluyen un campo de detalle.
 * Ej: "¿Tiene alguna enfermedad crónica?" -> active=true, specific="Diabetes".
 */
public record ConditionDetailDTO(

        @NotNull(message = "Debe indicar si aplica o no")
        Boolean active,

        String specific
) {
}
