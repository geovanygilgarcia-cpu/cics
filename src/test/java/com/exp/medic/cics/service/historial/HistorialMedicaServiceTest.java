package com.exp.medic.cics.service.historial;

import com.exp.medic.cics.dto.historial.request.PatientIntakeFormDTO;
import com.exp.medic.cics.dto.historial.response.PatientIntakeFormDTOResponse;
import com.exp.medic.cics.service.historial.impl.HistorialMedicaService;
import com.exp.medic.cics.service.historial.impl.HistorialMedicoAcroFormService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistorialMedicaServiceTest {

    @Mock
    private HistorialMedicoAcroFormService historialMedicoPdfService;

    @InjectMocks
    private HistorialMedicaService historialMedicaService;

    private PatientIntakeFormDTO patientIntakeFormDTO;
    private byte[] fakePdfBytes;

    @BeforeEach
    void setUp() {
        // Mock del DTO de entrada ya que no dependemos de sus campos concretos
        patientIntakeFormDTO = mock(PatientIntakeFormDTO.class);
        fakePdfBytes = "PDF-CONTENT-DE-PRUEBA".getBytes();
    }

    @Test
    void createdHistorialMedic_debeGenerarPdfYRetornarRespuestaCorrecta() {
        // given
        when(historialMedicoPdfService.generarPdf(patientIntakeFormDTO))
                .thenReturn(fakePdfBytes);

        try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
            filesMockedStatic.when(() -> Files.write(any(Path.class), any(byte[].class)))
                    .thenReturn(Path.of("historia_clinica_editable.pdf"));

            // when
            PatientIntakeFormDTOResponse response =
                    historialMedicaService.createdHistorialMedic(patientIntakeFormDTO);

            // then
            assertNotNull(response);
            assertTrue(response.success());
            assertEquals("historia-clinica-prueba.pdf", response.fileName());
            assertEquals(
                    Base64.getEncoder().encodeToString(fakePdfBytes),
                    response.pdfBase64()
            );

            verify(historialMedicoPdfService, times(1)).generarPdf(patientIntakeFormDTO);
            filesMockedStatic.verify(() -> Files.write(any(Path.class), eq(fakePdfBytes)));
        }
    }

    @Test
    void createdHistorialMedic_debeContinuarYRetornarRespuestaAunSiFallaEscrituraEnDisco() {
        // given: el guardado en disco lanza IOException, pero no debe romper el flujo
        when(historialMedicoPdfService.generarPdf(patientIntakeFormDTO))
                .thenReturn(fakePdfBytes);

        try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
            filesMockedStatic.when(() -> Files.write(any(Path.class), any(byte[].class)))
                    .thenThrow(new IOException("Disco lleno"));

            // when
            PatientIntakeFormDTOResponse response =
                    historialMedicaService.createdHistorialMedic(patientIntakeFormDTO);

            // then: el response se genera correctamente pese al error de escritura
            assertNotNull(response);
            assertTrue(response.success());
            assertEquals("historia-clinica-prueba.pdf", response.fileName());
            assertEquals(
                    Base64.getEncoder().encodeToString(fakePdfBytes),
                    response.pdfBase64()
            );

            verify(historialMedicoPdfService, times(1)).generarPdf(patientIntakeFormDTO);
        }
    }

    @Test
    void createdHistorialMedic_debePropagarExcepcionSiFallaGeneracionDePdf() {
        // given
        when(historialMedicoPdfService.generarPdf(patientIntakeFormDTO))
                .thenThrow(new RuntimeException("Error al generar el PDF"));

        // when / then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> historialMedicaService.createdHistorialMedic(patientIntakeFormDTO)
        );

        assertEquals("Error al generar el PDF", exception.getMessage());
        verify(historialMedicoPdfService, times(1)).generarPdf(patientIntakeFormDTO);
    }
}
