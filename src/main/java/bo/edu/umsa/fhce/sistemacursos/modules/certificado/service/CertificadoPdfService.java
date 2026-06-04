// src/main/java/.../modules/certificado/service/CertificadoPdfService.java

package bo.edu.umsa.fhce.sistemacursos.modules.certificado.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;

import bo.edu.umsa.fhce.sistemacursos.modules.certificado.entity.Certificado;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.repository.EvaluacionRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.entity.Inscripcion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificadoPdfService {

    private final QrService             qrService;
    private final EvaluacionRepository  evaluacionRepository;

    @Value("${app.certificados.directorio:certificados}")
    private String directorioCertificados;

    @Value("${app.email.base-url}")
    private String baseUrl;

    // ── Coordenadas fijas — carta horizontal (792 × 612 pt) ─────────────────
    // El diseñador adapta su plantilla a estas posiciones.
    // Origen (0,0) = esquina inferior izquierda.
    // x=0 con ancho=792 = texto centrado en toda la página.

    private static final float ANCHO_PAGINA   = 792f; // carta horizontal
    private static final float ALTO_PAGINA    = 612f;
    private static final int   NUM_PAGINA     = 1;

    // Posición Y de cada elemento (desde el borde inferior)
    private static final float Y_NOMBRE       = 330f; // nombre participante — centro
    private static final float Y_FECHA        = 150f; // fecha de emisión

    // QR — esquina inferior derecha
    private static final float QR_SIZE        = 80f;
    private static final float QR_X           = 672f; // 792 - 80 - 40 margen
    private static final float QR_Y           = 30f;

    // URL debajo del QR
    private static final float URL_X          = 570f;
    private static final float URL_Y          = 18f;
    private static final float URL_ANCHO      = 210f;

    // ── Métodos públicos ─────────────────────────────────────────────────────

    // Genera el PDF, lo guarda en disco y devuelve la ruta
    public String generarYGuardar(Certificado certificado, String rutaPlantilla) {
        try {
            byte[] pdfBytes = generarPdf(certificado, rutaPlantilla);

            Path dirPath = Paths.get(directorioCertificados);
            Files.createDirectories(dirPath);

            String nombreArchivo = String.format("cert_%d_v%d.pdf",
                certificado.getIdCertificado(),
                certificado.getVersion());

            Path rutaArchivo = dirPath.resolve(nombreArchivo);
            Files.write(rutaArchivo, pdfBytes);

            log.info("PDF generado: {}", rutaArchivo);
            return rutaArchivo.toString();

        } catch (IOException e) {
            log.error("Error guardando PDF del certificado {}",
                certificado.getIdCertificado(), e);
            throw new RuntimeException("Error generando certificado PDF", e);
        }
    }

    // Genera el PDF como bytes para descarga directa sin guardar en disco
    public byte[] generarParaDescarga(Certificado certificado, String rutaPlantilla) {
        try {
            return generarPdf(certificado, rutaPlantilla);
        } catch (IOException e) {
            throw new RuntimeException("Error generando PDF para descarga", e);
        }
    }

    // ── Generación interna ───────────────────────────────────────────────────

    private byte[] generarPdf(Certificado certificado,
                               String rutaPlantilla) throws IOException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Inscripcion inscripcion = certificado.getInscripcion();

        // Leer la plantilla del disco
        byte[] plantillaBytes = Files.readAllBytes(Paths.get(rutaPlantilla));

        PdfReader reader = new PdfReader(new ByteArrayInputStream(plantillaBytes));
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdfDoc = new PdfDocument(reader, writer);

        // Carta horizontal — sin márgenes, la plantilla ya los tiene
        Document document = new Document(pdfDoc, PageSize.LETTER.rotate());
        document.setMargins(0, 0, 0, 0);

        PdfFont fontBold = PdfFontFactory.createFont(
            com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
        PdfFont fontNormal = PdfFontFactory.createFont(
            com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

        // ── Nombre del participante ───────────────────────────────────────
        String nombreParticipante =
            inscripcion.getParticipante().getNombres().toUpperCase()
            + " " + inscripcion.getParticipante().getApellidos().toUpperCase();

        document.add(new Paragraph(nombreParticipante)
            .setFont(fontBold)
            .setFontSize(28)
            .setTextAlignment(TextAlignment.CENTER)
            .setFixedPosition(NUM_PAGINA, 0, Y_NOMBRE, ANCHO_PAGINA));

        // ── Fecha de emisión ──────────────────────────────────────────────
        String fecha = certificado.getFechaEmision()
            .format(DateTimeFormatter.ofPattern(
                "dd 'de' MMMM 'de' yyyy", new Locale("es", "BO")));

        document.add(new Paragraph("La Paz, " + fecha)
            .setFont(fontNormal)
            .setFontSize(11)
            .setTextAlignment(TextAlignment.CENTER)
            .setFixedPosition(NUM_PAGINA, 0, Y_FECHA, ANCHO_PAGINA));

        // ── Código QR — esquina inferior derecha ──────────────────────────
        String urlVerificacion = baseUrl + "/verificar/"
            + certificado.getCodigoVerificacion();

        byte[] qrBytes = qrService.generarQr(urlVerificacion, 200);
        Image qrImage = new Image(ImageDataFactory.create(qrBytes));
        qrImage.setWidth(QR_SIZE).setHeight(QR_SIZE);
        qrImage.setFixedPosition(NUM_PAGINA, QR_X, QR_Y);
        document.add(qrImage);

        // URL de verificación debajo del QR — texto pequeño
        document.add(new Paragraph(urlVerificacion)
            .setFont(fontNormal)
            .setFontSize(6)
            .setFontColor(ColorConstants.GRAY)
            .setFixedPosition(NUM_PAGINA, URL_X, URL_Y, URL_ANCHO));

        document.close();
        return outputStream.toByteArray();
    }
}