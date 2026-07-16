package com.exp.medic.cics.service.receta;

import com.exp.medic.cics.dto.receta.request.FechaDTO;
import com.exp.medic.cics.dto.receta.request.RecetaDTO;
import com.exp.medic.cics.dto.receta.request.SignosVitalesDTO;
import com.exp.medic.cics.service.receta.impl.RecetaMedicaAcroFormService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecetaMedicaAcroFormServiceTest {

    private static final String TEMPLATE_RELATIVE_PATH = "templates/receta_editable.pdf";

    private final RecetaMedicaAcroFormService service = new RecetaMedicaAcroFormService();

    private static Path templateFileEnClasspath;
    private static byte[] backupPlantillaOriginal;

    private static final List<String> CAMPOS_TEXTO = List.of(
            "folio", "paciente_fullname", "receta_edad",
            "dia", "mes", "ano",
            "peso", "talla", "ta", "fc", "fr", "temp", "sato2", "imc", "alergias",
            "idx", "proxima_cita"
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
        byte[] pdfConAcroForm = construirPdfConAcroForm(CAMPOS_TEXTO);
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
    void llenarFormulario_conFechaNula_noDebeLanzarExcepcionYDebeOmitirEsosCampos() {
        RecetaDTO receta = new RecetaDTO(
                "F-2026-00124",
                "Ana López",
                "30",
                null,
                buildSignosVitales(),
                "Diagnóstico de prueba",
                "Tratamiento de prueba",
                "20/08/2026",
                "Dr. Fredy Gil García"
        );

        byte[] resultado = assertDoesNotThrow(() -> service.llenarFormulario(receta));
        assertNotNull(resultado);
    }

    @Test
    void llenarFormulario_conSignosVitalesNulos_noDebeLanzarExcepcion() {
        RecetaDTO receta = new RecetaDTO(
                "F-2026-00125",
                "Ana López",
                "30",
                new FechaDTO("20", "08", "2026"),
                null,
                "Diagnóstico de prueba",
                "Tratamiento de prueba",
                "20/09/2026",
                "Dr. Fredy Gil García"
        );

        byte[] resultado = assertDoesNotThrow(() -> service.llenarFormulario(receta));
        assertNotNull(resultado);
    }

    @Test
    void llenarFormulario_conCamposDeTextoNulos_noDebeLanzarExcepcion() {
        RecetaDTO receta = new RecetaDTO(
                null, null, null, null, null, null, null, null, null
        );

        byte[] resultado = assertDoesNotThrow(() -> service.llenarFormulario(receta));
        assertNotNull(resultado);
    }

    @Test
    void llenarFormulario_conPdfSinAcroForm_debeLanzarIllegalStateException() throws IOException {
        byte[] respaldo = Files.readAllBytes(templateFileEnClasspath);
        try {
            Files.write(templateFileEnClasspath, construirPdfSinAcroForm());

            RecetaDTO receta = buildRecetaCompleta();

            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> service.llenarFormulario(receta)
            );

            assertEquals("El PDF no contiene un AcroForm", ex.getMessage());
        } finally {
            Files.write(templateFileEnClasspath, respaldo);
        }
    }

    // -------------------- Helpers de construcción de DTO --------------------

    private RecetaDTO buildRecetaCompleta() {
        return new RecetaDTO(
                "F-2026-00123",
                "Juan Pérez Hernández",
                "45",
                new FechaDTO("12", "07", "2026"),
                buildSignosVitales(),
                "Gonartrosis derecha grado II",
                "Manejo con AINE, fisioterapia 2 veces por semana durante 4 semanas.",
                "12/08/2026",
                "Dr. Fredy Gil García"
        );
    }

    private SignosVitalesDTO buildSignosVitales() {
        return new SignosVitalesDTO(
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
    }

    // -------------------- Helpers de generación de PDFs de prueba --------------------

    private static byte[] construirPdfConAcroForm(List<String> camposTexto) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDAcroForm acroForm = new PDAcroForm(document);
            document.getDocumentCatalog().setAcroForm(acroForm);
            acroForm.setDefaultResources(new PDResources());
            acroForm.setDefaultAppearance("/Helv 10 Tf 0 g");

            float y = 800;
            for (String nombre : camposTexto) {
                PDTextField textField = new PDTextField(acroForm);
                textField.setPartialName(nombre);
                agregarWidget(page, acroForm, textField, y);
                y -= 20;
                if (y < 20) {
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    y = 800;
                }
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
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}
