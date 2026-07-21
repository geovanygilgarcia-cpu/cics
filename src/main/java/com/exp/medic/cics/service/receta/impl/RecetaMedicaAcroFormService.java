package com.exp.medic.cics.service.receta.impl;

import com.exp.medic.cics.dto.receta.request.RecetaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecetaMedicaAcroFormService  {

    private static final String TEMPLATE_PATH = "templates/receta_editable_2.pdf";

    public byte[] llenarFormulario(RecetaDTO recetaDTO) {
        try (InputStream templateStream = new ClassPathResource(TEMPLATE_PATH).getInputStream();
             PDDocument document = Loader.loadPDF(templateStream.readAllBytes())) {

            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

            if (acroForm == null) {
                throw new IllegalStateException("El PDF no contiene un AcroForm");
            }

            setField(acroForm, "folio", recetaDTO.folio());
            setField(acroForm, "paciente_fullname", recetaDTO.paciente());
            setField(acroForm, "receta_edad", recetaDTO.edad());

            if (recetaDTO.fecha() != null) {
                setField(acroForm, "dia", recetaDTO.fecha().dia());
                setField(acroForm, "mes", recetaDTO.fecha().mes());
                setField(acroForm, "ano", recetaDTO.fecha().anio());
            }

            if (recetaDTO.signosVitales() != null) {
                var sv = recetaDTO.signosVitales();
                setField(acroForm, "peso", sv.peso());
                setField(acroForm, "talla", sv.talla());
                setField(acroForm, "ta", sv.ta());
                setField(acroForm, "fc", sv.fc());
                setField(acroForm, "fr", sv.fr());
                setField(acroForm, "temp", sv.temp());
                setField(acroForm, "sato2", sv.sato2());
                setField(acroForm, "imc", sv.imc());
                setField(acroForm, "alergias", sv.alergias());
            }

            setField(acroForm, "idx", recetaDTO.idx());

            // Aseguramos que el campo acepte multilínea antes de llenarlo
            asegurarMultilinea(acroForm, "tratamiento");
            setField(acroForm, "tratamiento", formatearMedicamentos(recetaDTO.diagnosticoTratamiento()));

            setField(acroForm, "proxima_cita", recetaDTO.proximaCita());

            acroForm.flatten();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("Error al llenar el formulario de receta médica", e);
            throw new RuntimeException("No se pudo generar el PDF de la receta", e);
        }
    }

    private void setField(PDAcroForm acroForm, String fieldName, String value) {
        try {
            PDField field = acroForm.getField(fieldName);
            if (field != null && value != null) {
                field.setValue(value);
            } else if (field == null) {
                log.warn("Campo '{}' no encontrado en el AcroForm", fieldName);
            }
        } catch (IOException e) {
            log.error("Error al asignar valor al campo '{}'", fieldName, e);
        }
    }

    /**
     * Convierte el string de diagnosticoTratamiento (donde cada '\n'
     * representa un medicamento/tratamiento distinto) en una lista
     * numerada lista para mostrarse en el campo multilínea del PDF.
     */
    private String formatearMedicamentos(String diagnosticoTratamiento) {
        if (diagnosticoTratamiento == null || diagnosticoTratamiento.isBlank()) {
            return "";
        }

        String[] lineas = diagnosticoTratamiento.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();

        int contador = 1;
        for (String linea : lineas) {
            String limpio = linea.trim();
            if (limpio.isEmpty()) continue;

            if (contador > 1) {
                sb.append(System.lineSeparator()).append(System.lineSeparator()); // <-- línea extra entre medicamentos
            }
            sb.append(contador++).append(". ").append(limpio);
        }

        return sb.toString();
    }

    /**
     * Garantiza que el campo sea multilínea; si no lo es, lo activa
     * en tiempo de ejecución para que los saltos de línea se respeten.
     */
    private void asegurarMultilinea(PDAcroForm acroForm, String fieldName) {
        try {
            PDField field = acroForm.getField(fieldName);
            if (field instanceof PDTextField textField && !textField.isMultiline()) {
                textField.setMultiline(true);
                log.info("Campo '{}' configurado como multilínea", fieldName);
            }
        } catch (Exception e) {
            log.warn("No se pudo verificar/ajustar multilínea en '{}'", fieldName, e);
        }
    }
}