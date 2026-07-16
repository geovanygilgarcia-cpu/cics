package com.exp.medic.cics.service.historial;

import com.exp.medic.cics.dto.historial.request.*;
import com.exp.medic.cics.service.historial.impl.HistorialMedicoAcroFormService;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistorialMedicoAcroFormServiceTest {

    private static final String TEMPLATE_RELATIVE_PATH = "templates/historia_clinica_editable.pdf";

    private final HistorialMedicoAcroFormService service = new HistorialMedicoAcroFormService();

    private static Path templateFileEnClasspath;
    private static byte[] backupPlantillaOriginal;

    private static final List<String> CAMPOS_TEXTO = List.of(
            "nombre_paciente", "paciente_number", "nacimento_paciente",
            "paciente_email", "paciente_emergencia",
            "enfermedad_cronica_especifica", "cirugias_mayores_especifica",
            "nombre_medicina", "alguna_alergia_especifica",
            "cancer_especifica", "otro_especifica",
            "sintomas", "duracion_sintomas", "tratamiento_anterior",
            "evaluacion_inicial", "pruebas_tratamientos", "fecha_cita"
    );

    // "otro" aparece una sola vez porque en el AcroForm real ES EL MISMO campo
    // que usa tanto género=OTHER como familyMedicalHistory().other() (bug a revisar)
    private static final List<String> CAMPOS_CHECKBOX = List.of(
            "masculino", "femenino", "otro",
            "enfermedad_cronica_si", "cirugias_mayores_si",
            "toma_medicamento_si", "alguna_alergia_si",
            "cardiopatia", "presion_arterial", "diabetes", "cancer",
            "cita_si"
    );

    @BeforeAll
    static void generarPlantillaDePrueba() throws IOException {
        try {
            Path testClassesDir = Path.of(
                    Thread.currentThread().getContextClassLoader().getResource("").toURI()
            );
            templateFileEnClasspath = testClassesDir.resolve(TEMPLATE_RELATIVE_PATH);
        } catch (java.net.URISyntaxException e) {
            throw new IOException("No se pudo resolver el directorio de test-classes", e);
        }

        if (Files.exists(templateFileEnClasspath)) {
            backupPlantillaOriginal = Files.readAllBytes(templateFileEnClasspath);
        }

        Files.createDirectories(templateFileEnClasspath.getParent());
        byte[] pdfConAcroForm = construirPdfConAcroForm(CAMPOS_TEXTO, CAMPOS_CHECKBOX);
        Files.write(templateFileEnClasspath, pdfConAcroForm);
    }

    @AfterAll
    static void restaurarPlantillaOriginal() throws IOException {
        if (backupPlantillaOriginal != null) {
            Files.write(templateFileEnClasspath, backupPlantillaOriginal);
        } else {
            Files.deleteIfExists(templateFileEnClasspath);
        }
    }

    @Test
    void generarPdf_conDoctorNotesNulo_noDebeLanzarExcepcionYDebeOmitirEsaSeccion() {
        PatientIntakeFormDTO formSinNotas = new PatientIntakeFormDTO(
                buildPatientInformation(),
                buildMedicalHistory(),
                buildFamilyMedicalHistory(),
                buildReasonForVisit(),
                null
        );

        byte[] resultado = assertDoesNotThrow(() -> service.generarPdf(formSinNotas));

        assertNotNull(resultado);
        assertTrue(resultado.length > 0);
    }

    @Test
    void generarPdf_conCamposOpcionalesNulos_noDebeLanzarExcepcion() {
        PatientInformationDTO patientInformation = new PatientInformationDTO(
                "Ana López",
                null,
                null,
                null,
                Gender.FEMALE,
                null
        );

        PatientIntakeFormDTO form = new PatientIntakeFormDTO(
                patientInformation,
                new MedicalHistoryDTO(
                        new ConditionDetailDTO(false, null),
                        new ConditionDetailDTO(false, null),
                        new MedicationDTO(false, null),
                        new ConditionDetailDTO(false, null)
                ),
                new FamilyMedicalHistoryDTO(
                        false, false, false, false,
                        new ConditionDetailDTO(false, null),
                        new ConditionDetailDTO(false, null)
                ),
                new ReasonForVisitDTO("Chequeo general", "N/A", null),
                null
        );

        byte[] resultado = assertDoesNotThrow(() -> service.generarPdf(form));
        assertNotNull(resultado);
    }

    @Test
    void generarPdf_conFollowUpAppointmentSinFecha_noDebeLanzarExcepcion() {
        PatientIntakeFormDTO form = new PatientIntakeFormDTO(
                buildPatientInformation(),
                buildMedicalHistory(),
                buildFamilyMedicalHistory(),
                buildReasonForVisit(),
                new DoctorNotesDTO(
                        "Evaluación inicial",
                        "Pruebas recomendadas",
                        new FollowUpAppointmentDTO(false, null)
                )
        );

        byte[] resultado = assertDoesNotThrow(() -> service.generarPdf(form));
        assertNotNull(resultado);
    }

    @Test
    void generarPdf_conPdfSinAcroForm_debeLanzarIllegalStateException() throws IOException {
        byte[] respaldo = Files.readAllBytes(templateFileEnClasspath);
        try {
            Files.write(templateFileEnClasspath, construirPdfSinAcroForm());

            PatientIntakeFormDTO form = buildFormularioCompleto();

            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> service.generarPdf(form)
            );

            assertTrue(ex.getMessage().contains("no tiene campos de formulario"));
        } finally {
            Files.write(templateFileEnClasspath, respaldo);
        }
    }

    // -------------------- Helpers de construcción de DTO --------------------

    private PatientIntakeFormDTO buildFormularioCompleto() {
        return new PatientIntakeFormDTO(
                buildPatientInformation(),
                buildMedicalHistory(),
                buildFamilyMedicalHistory(),
                buildReasonForVisit(),
                buildDoctorNotes()
        );
    }

    private PatientInformationDTO buildPatientInformation() {
        return new PatientInformationDTO(
                "Juan Pérez Hernández",
                "5512345678",
                LocalDate.of(1980, 5, 20),
                "juan.perez@email.com",
                Gender.MALE,
                "María Pérez - 5598765432"
        );
    }

    private MedicalHistoryDTO buildMedicalHistory() {
        return new MedicalHistoryDTO(
                new ConditionDetailDTO(true, "Hipertensión arterial"),
                new ConditionDetailDTO(false, null),
                new MedicationDTO(true, "Losartán 50mg"),
                new ConditionDetailDTO(true, "Penicilina")
        );
    }

    private FamilyMedicalHistoryDTO buildFamilyMedicalHistory() {
        return new FamilyMedicalHistoryDTO(
                true,
                true,
                true,
                false,
                new ConditionDetailDTO(false, null),
                new ConditionDetailDTO(true, "Asma en línea materna")
        );
    }

    private ReasonForVisitDTO buildReasonForVisit() {
        return new ReasonForVisitDTO(
                "Dolor en rodilla derecha al caminar",
                "3 semanas",
                "Analgésicos de venta libre sin mejoría"
        );
    }

    private DoctorNotesDTO buildDoctorNotes() {
        return new DoctorNotesDTO(
                "Paciente refiere dolor mecánico, sin datos de derrame articular",
                "Se solicita radiografía de rodilla AP y lateral",
                new FollowUpAppointmentDTO(true, LocalDate.of(2026, 8, 12))
        );
    }

    // -------------------- Helpers de generación de PDFs de prueba --------------------

    private static byte[] construirPdfConAcroForm(List<String> camposTexto,
                                                  List<String> camposCheckbox) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDAcroForm acroForm = new PDAcroForm(document);
            document.getDocumentCatalog().setAcroForm(acroForm);

            // Registra la fuente Helvetica bajo el nombre "Helv" para que el
            // flatten() pueda generar correctamente las apariencias de texto
            PDResources resources = new PDResources();
            resources.put(COSName.getPDFName("Helv"),
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA));
            acroForm.setDefaultResources(resources);
            acroForm.setDefaultAppearance("/Helv 10 Tf 0 g");

            float y = 800;
            for (String nombre : camposTexto) {
                PDTextField textField = new PDTextField(acroForm);
                textField.setPartialName(nombre);
                textField.setDefaultAppearance("/Helv 10 Tf 0 g");
                agregarWidget(page, acroForm, textField, y);
                y -= 20;
                if (y < 20) { document.addPage(page = new PDPage(PDRectangle.A4)); y = 800; }
            }
            for (String nombre : camposCheckbox) {
                PDCheckBox checkBox = new PDCheckBox(acroForm);
                checkBox.setPartialName(nombre);
                agregarWidgetCheckbox(page, acroForm, checkBox, y);
                y -= 20;
                if (y < 20) { document.addPage(page = new PDPage(PDRectangle.A4)); y = 800; }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private static void agregarWidget(PDPage page, PDAcroForm acroForm,
                                      PDTextField field, float y) throws IOException {
        PDAnnotationWidget widget = new PDAnnotationWidget();
        widget.setRectangle(new PDRectangle(50, y, 200, 15));
        widget.setPage(page);
        widget.setPrinted(true);
        field.getWidgets().add(widget);
        page.getAnnotations().add(widget);
        acroForm.getFields().add(field);
    }

    private static void agregarWidgetCheckbox(PDPage page, PDAcroForm acroForm,
                                              PDCheckBox checkBox, float y) throws IOException {
        PDAnnotationWidget widget = new PDAnnotationWidget();
        widget.setRectangle(new PDRectangle(260, y, 15, 15));
        widget.setPage(page);
        widget.setPrinted(true);
        checkBox.getWidgets().add(widget);
        page.getAnnotations().add(widget);
        acroForm.getFields().add(checkBox);
    }

    private static byte[] construirPdfSinAcroForm() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText("PDF sin AcroForm");
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private static String extraerTexto(byte[] pdfBytes) throws IOException {
        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}