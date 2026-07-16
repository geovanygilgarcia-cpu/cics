package com.exp.medic.cics.dto.historial.response;

public record PatientIntakeFormDTOResponse(
        boolean success,
        String fileName,
        String pdfBase64
) {
}