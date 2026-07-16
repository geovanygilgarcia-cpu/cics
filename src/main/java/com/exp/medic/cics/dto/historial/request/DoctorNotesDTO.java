package com.exp.medic.cics.dto.historial.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Campos que solo debe poder escribir el rol MEDICO.
 * Validar con @PreAuthorize("hasRole('MEDICO')") en el controller/service.
 */
public record DoctorNotesDTO(

        String initialEvaluation,

        String recommendedTestsOrTreatments,

        @NotNull @Valid
        FollowUpAppointmentDTO followUpAppointment
) {
}
