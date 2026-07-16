package com.exp.medic.cics.dto.historial.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record FollowUpAppointmentDTO(

        @NotNull(message = "Debe indicar si requiere cita de seguimiento")
        Boolean active,

        @FutureOrPresent(message = "La fecha de la cita no puede ser en el pasado")
        LocalDate date
) {
}
