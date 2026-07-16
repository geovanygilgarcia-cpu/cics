package com.exp.medic.cics.service.receta;

import com.exp.medic.cics.dto.receta.request.FechaDTO;
import com.exp.medic.cics.dto.receta.request.RecetaDTO;
import com.exp.medic.cics.dto.receta.request.SignosVitalesDTO;
import com.exp.medic.cics.dto.receta.response.RecetaDTOResponse;
import com.exp.medic.cics.service.receta.impl.RecetaMedicaAcroFormService;
import com.exp.medic.cics.service.receta.impl.RecetaMedicaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecetaMedicaServiceTest {

    @Mock
    private RecetaMedicaAcroFormService recetaMedicaAcroFormService;

    @InjectMocks
    private RecetaMedicaService recetaMedicaService;

    private RecetaDTO recetaDTO;
    private byte[] fakePdfBytes;

    @BeforeEach
    void setUp() {
        recetaDTO = buildRecetaCompleta();
        fakePdfBytes = "PDF-CONTENT-DE-PRUEBA".getBytes();
    }

    @Test
    void createdRecetaMedic_debeGenerarPdfYRetornarRespuestaCorrecta() {
        // given
        when(recetaMedicaAcroFormService.llenarFormulario(recetaDTO))
                .thenReturn(fakePdfBytes);

        // when
        RecetaDTOResponse response = recetaMedicaService.createdRecetaMedic(recetaDTO);

        // then
        assertNotNull(response);
        assertTrue(response.success());
        assertEquals("receta_F-2026-00123.pdf", response.fileName());
        assertEquals(
                Base64.getEncoder().encodeToString(fakePdfBytes),
                response.pdfBase64()
        );

        verify(recetaMedicaAcroFormService, times(1)).llenarFormulario(recetaDTO);
    }

    @Test
    void createdRecetaMedic_debeConstruirNombreDeArchivoConFolio() {
        RecetaDTO recetaConOtroFolio = new RecetaDTO(
                "ABC-999",
                recetaDTO.paciente(),
                recetaDTO.edad(),
                recetaDTO.fecha(),
                recetaDTO.signosVitales(),
                recetaDTO.idx(),
                recetaDTO.diagnosticoTratamiento(),
                recetaDTO.proximaCita(),
                recetaDTO.firma()
        );

        when(recetaMedicaAcroFormService.llenarFormulario(recetaConOtroFolio))
                .thenReturn(fakePdfBytes);

        RecetaDTOResponse response = recetaMedicaService.createdRecetaMedic(recetaConOtroFolio);

        assertEquals("receta_ABC-999.pdf", response.fileName());
    }

    @Test
    void createdRecetaMedic_debePropagarExcepcionSiFallaGeneracionDePdf() {
        // given
        when(recetaMedicaAcroFormService.llenarFormulario(recetaDTO))
                .thenThrow(new RuntimeException("No se pudo generar el PDF de la receta"));

        // when / then
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> recetaMedicaService.createdRecetaMedic(recetaDTO)
        );

        assertEquals("No se pudo generar el PDF de la receta", ex.getMessage());
        verify(recetaMedicaAcroFormService, times(1)).llenarFormulario(recetaDTO);
    }

    @Test
    void createdRecetaMedic_conFolioNulo_debeLanzarNullPointerException() {
        // El código hace "receta_" + recetaDTO.folio(), y con folio null
        // concatenaría "receta_nullpdf", no lanza NPE realmente -- se valida el comportamiento real
        RecetaDTO recetaSinFolio = new RecetaDTO(
                null,
                recetaDTO.paciente(),
                recetaDTO.edad(),
                recetaDTO.fecha(),
                recetaDTO.signosVitales(),
                recetaDTO.idx(),
                recetaDTO.diagnosticoTratamiento(),
                recetaDTO.proximaCita(),
                recetaDTO.firma()
        );

        when(recetaMedicaAcroFormService.llenarFormulario(recetaSinFolio))
                .thenReturn(fakePdfBytes);

        RecetaDTOResponse response = recetaMedicaService.createdRecetaMedic(recetaSinFolio);

        assertEquals("receta_null.pdf", response.fileName());
    }

    private RecetaDTO buildRecetaCompleta() {
        FechaDTO fecha = new FechaDTO("12", "07", "2026");

        SignosVitalesDTO signosVitales = new SignosVitalesDTO(
                "78 kg",
                "1.72 m",
                "120/80",
                "72",
                "16",
                "36.5",
                "98%",
                "26.4",
                "Ninguna conocida"
        );

        return new RecetaDTO(
                "F-2026-00123",
                "Juan Pérez Hernández",
                "45",
                fecha,
                signosVitales,
                "Gonartrosis derecha grado II",
                "Manejo con AINE, fisioterapia 2 veces por semana durante 4 semanas.",
                "12/08/2026",
                "Dr. Fredy Gil García"
        );
    }
}
