package com.exp.medic.cics.controller.pdf;

import com.exp.medic.cics.dto.historial.request.PatientIntakeFormDTO;
import com.exp.medic.cics.dto.historial.response.PatientIntakeFormDTOResponse;
import com.exp.medic.cics.service.historial.IHistorialMedicaService;
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
public class HistorialMedicoController {

    private final IHistorialMedicaService historialMedicaService;

    @PostMapping(value = "/historia-clinica", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> createdHistorialMedic(
            @Valid @RequestBody PatientIntakeFormDTO patientIntakeFormDTO) {

        PatientIntakeFormDTOResponse response = historialMedicaService.createdHistorialMedic(patientIntakeFormDTO);
        byte[] pdfBytes = Base64.getDecoder().decode(response.pdfBase64());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + response.fileName())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}