package com.exp.medic.cics.service.receta.impl;

import com.exp.medic.cics.dto.receta.request.RecetaDTO;
import com.exp.medic.cics.dto.receta.response.RecetaDTOResponse;
import com.exp.medic.cics.service.receta.IRecetaMedicaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RecetaMedicaService implements IRecetaMedicaService {

    private final RecetaMedicaAcroFormService recetaMedicaAcroFormService;

    @Override
    public RecetaDTOResponse createdRecetaMedic(RecetaDTO recetaDTO) {
        byte[] pdfBytes = recetaMedicaAcroFormService.llenarFormulario(recetaDTO);
        String pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);
        String fileName = "receta_" + recetaDTO.folio() + ".pdf";

        return new RecetaDTOResponse(
                true,
                fileName,
                pdfBase64
        );
    }

}
