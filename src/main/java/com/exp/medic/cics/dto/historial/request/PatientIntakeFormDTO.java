package com.exp.medic.cics.dto.historial.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record PatientIntakeFormDTO(

        @NotNull @Valid
        PatientInformationDTO patientInformation,

        @NotNull @Valid
        MedicalHistoryDTO medicalHistory,

        @NotNull @Valid
        FamilyMedicalHistoryDTO familyMedicalHistory,

        @NotNull @Valid
        ReasonForVisitDTO reasonForVisit,

        @Valid
        DoctorNotesDTO doctorNotes
) {
}
