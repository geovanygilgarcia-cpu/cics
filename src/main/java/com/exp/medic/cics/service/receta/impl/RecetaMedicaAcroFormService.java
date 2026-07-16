package com.exp.medic.cics.service.receta.impl;

import com.exp.medic.cics.dto.receta.request.RecetaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecetaMedicaAcroFormService  {

    private static final String TEMPLATE_PATH = "templates/receta_editable.pdf";

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

            setField(acroForm, "idx", recetaDTO.diagnosticoTratamiento());
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
}