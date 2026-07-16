package com.exp.medic.cics.service.historial.impl;

import com.exp.medic.cics.dto.historial.request.PatientIntakeFormDTO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;

@Service
public class HistorialMedicoAcroFormService {

    private static final String TEMPLATE_PATH = "templates/historia_clinica_editable.pdf";
    private static final DateTimeFormatter FECHA_FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generarPdf(PatientIntakeFormDTO form) {
        try (InputStream templateStream = new ClassPathResource(TEMPLATE_PATH).getInputStream()) {

            PDDocument document = Loader.loadPDF(templateStream.readAllBytes());
            try (document) {

                PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
                if (acroForm == null) {
                    throw new IllegalStateException(
                            "Este PDF no tiene campos de formulario. Conviértelo primero con LibreOffice/Acrobat.");
                }

                // ---- Información del paciente ----
                setTexto(acroForm, "nombre_paciente", form.patientInformation().fullName());
                setTexto(acroForm, "paciente_number", form.patientInformation().phone());
                setTexto(acroForm, "nacimento_paciente",
                        form.patientInformation().dateOfBirth() != null
                                ? form.patientInformation().dateOfBirth().format(FECHA_FORMATO) : "");
                setTexto(acroForm, "paciente_email", form.patientInformation().email());
                setTexto(acroForm, "paciente_emergencia", form.patientInformation().emergencyContact());
                setCheckbox(acroForm, "masculino", "MALE".equals(form.patientInformation().gender().name()));
                setCheckbox(acroForm, "femenino", "FEMALE".equals(form.patientInformation().gender().name()));
                setCheckbox(acroForm, "otro", "OTHER".equals(form.patientInformation().gender().name()));

                // ---- Historial médico ----
                setCheckbox(acroForm, "enfermedad_cronica_si", form.medicalHistory().hasChronicDisease().active());
                setTexto(acroForm, "enfermedad_cronica_especifica", form.medicalHistory().hasChronicDisease().specific());

                setCheckbox(acroForm, "cirugias_mayores_si", form.medicalHistory().hasHadMajorSurgeries().active());
                setTexto(acroForm, "cirugias_mayores_especifica", form.medicalHistory().hasHadMajorSurgeries().specific());

                setCheckbox(acroForm, "toma_medicamento_si", form.medicalHistory().takesMedication().active());
                setTexto(acroForm, "nombre_medicina", form.medicalHistory().takesMedication().medicationName());

                setCheckbox(acroForm, "alguna_alergia_si", form.medicalHistory().hasAllergies().active());
                setTexto(acroForm, "alguna_alergia_especifica", form.medicalHistory().hasAllergies().specific());

                // ---- Historia médica familiar ----
                setCheckbox(acroForm, "cardiopatia", form.familyMedicalHistory().heartDisease());
                setCheckbox(acroForm, "presion_arterial", form.familyMedicalHistory().highBloodPressure());
                setCheckbox(acroForm, "diabetes", form.familyMedicalHistory().diabetes());
                setCheckbox(acroForm, "cancer", form.familyMedicalHistory().cancer().active());
                setTexto(acroForm, "cancer_especifica", form.familyMedicalHistory().cancer().specific());
                setCheckbox(acroForm, "otro", form.familyMedicalHistory().other().active());
                setTexto(acroForm, "otro_especifica", form.familyMedicalHistory().other().specific());


                // ---- Razón de la visita ----
                setTexto(acroForm, "sintomas", form.reasonForVisit().symptoms());
                setTexto(acroForm, "duracion_sintomas", form.reasonForVisit().symptomDuration());
                setTexto(acroForm, "tratamiento_anterior", form.reasonForVisit().previousTreatment());

                // ---- Notas del médico ----
                if (form.doctorNotes() != null) {
                    setTexto(acroForm, "evaluacion_inicial", form.doctorNotes().initialEvaluation());
                    setTexto(acroForm, "pruebas_tratamientos", form.doctorNotes().recommendedTestsOrTreatments());
                    setCheckbox(acroForm, "cita_si", form.doctorNotes().followUpAppointment().active());
                    if (form.doctorNotes().followUpAppointment().date() != null) {
                        setTexto(acroForm, "fecha_cita",
                                form.doctorNotes().followUpAppointment().date().format(FECHA_FORMATO));
                    }
                }

                // "Aplana" el formulario: convierte los campos rellenados en texto fijo,
                // para que ya no se puedan seguir editando en el PDF final.
                acroForm.flatten();

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                document.save(out);
                return out.toByteArray();
            }

        } catch (IOException e) {
            throw new RuntimeException("Error generando el PDF de historial médico", e);
        }
    }

    private void setTexto(PDAcroForm acroForm, String nombreCampo, String valor) throws IOException {
        if (valor == null) return;
        PDField field = acroForm.getField(nombreCampo);
        if (field instanceof PDTextField textField) {
            textField.setValue(valor);
        }
    }

    private void setCheckbox(PDAcroForm acroForm, String nombreCampo, Boolean marcado) throws IOException {
        if (!Boolean.TRUE.equals(marcado)) return;
        PDField field = acroForm.getField(nombreCampo);
        if (field instanceof PDCheckBox checkBox) {
            checkBox.check();
        }
    }
}
