package com.exp.medic.cics.dto.receta.response;

public record RecetaDTOResponse(
        boolean success,
        String fileName,
        String pdfBase64
) {
}
