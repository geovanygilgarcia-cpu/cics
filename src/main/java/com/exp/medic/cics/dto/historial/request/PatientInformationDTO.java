package com.exp.medic.cics.dto.historial.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record PatientInformationDTO(

        @NotBlank(message = "El nombre completo es obligatorio")
        String fullName,

        @NotBlank(message = "El teléfono es obligatorio")
        String phone,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser en el pasado")
        LocalDate dateOfBirth,

        @Email(message = "El correo no tiene un formato válido")
        String email,

        @NotNull(message = "El género es obligatorio")
        Gender gender,

        String emergencyContact
) {
}
