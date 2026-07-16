package com.exp.medic.cics.controller.pdf;

import com.exp.medic.cics.dto.receta.request.RecetaDTO;
import com.exp.medic.cics.dto.receta.response.RecetaDTOResponse;
import com.exp.medic.cics.service.receta.IRecetaMedicaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;

@RestController
@RequestMapping("/api/expedientes")
@RequiredArgsConstructor
public class RecetaMedicaController {

    private final IRecetaMedicaService iRecetaMedicaService;

    @PostMapping(value = "/receta-medica", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> createdHistorialMedic(@Valid @RequestBody RecetaDTO recetaDTO) {

        RecetaDTOResponse response = iRecetaMedicaService.createdRecetaMedic(recetaDTO);
        byte[] pdfBytes = Base64.getDecoder().decode(response.pdfBase64());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + response.fileName())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
