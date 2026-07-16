package com.exp.medic.cics.service.historial.impl;

import com.exp.medic.cics.dto.historial.request.PatientIntakeFormDTO;
import com.exp.medic.cics.dto.historial.response.PatientIntakeFormDTOResponse;
import com.exp.medic.cics.service.historial.IHistorialMedicaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistorialMedicaService implements IHistorialMedicaService {

    private final HistorialMedicoAcroFormService historialMedicoPdfService;

    @Override
    public PatientIntakeFormDTOResponse createdHistorialMedic(PatientIntakeFormDTO patientIntakeFormDTO) {

        // 1. Genera el PDF a partir del DTO que llega en el request
        byte[] pdfBytes = historialMedicoPdfService.generarPdf(patientIntakeFormDTO);

        // 2. SOLO PARA PRUEBA: guarda una copia física en disco para que la abras y veas el resultado.
        //    Quita este bloque cuando ya lo tengas conectado a documentos-service o al front.
        try {
            Path rutaPrueba = Path.of("historia_clinica_editable.pdf");
            Files.write(rutaPrueba, pdfBytes);
            log.info("PDF de prueba generado en: {}", rutaPrueba.toAbsolutePath());
        } catch (IOException e) {
            log.error("No se pudo guardar el PDF de prueba", e);
        }

        // 3. Arma la respuesta
        return new PatientIntakeFormDTOResponse(
                true,
                "historia-clinica-prueba.pdf",
                Base64.getEncoder().encodeToString(pdfBytes)
        );
    }
}