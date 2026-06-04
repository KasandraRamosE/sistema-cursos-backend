package bo.edu.umsa.fhce.sistemacursos.modules.reporte.service;

import java.math.BigDecimal;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.event.AbstractPdfDocumentEventHandler;
import com.itextpdf.kernel.pdf.event.AbstractPdfDocumentEvent;
import com.itextpdf.kernel.pdf.event.PdfDocumentEvent;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.properties.BorderRadius;
import com.itextpdf.layout.properties.HorizontalAlignment;

import bo.edu.umsa.fhce.sistemacursos.exception.BusinessException;
import bo.edu.umsa.fhce.sistemacursos.exception.ResourceNotFoundException;
import bo.edu.umsa.fhce.sistemacursos.modules.carrera.repository.CoordinadorCarreraRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.entity.Curso;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.repository.CursoRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.repository.ParaleloRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.repository.AsistenciaRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.repository.EvaluacionRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.evento.entity.Evento;
import bo.edu.umsa.fhce.sistemacursos.modules.evento.repository.EventoRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.entity.Inscripcion;
import bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.repository.InscripcionRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.reporte.dto.ReporteActividadDetalleDto;
import bo.edu.umsa.fhce.sistemacursos.modules.reporte.dto.ReporteAcademicoCursoDto;
import bo.edu.umsa.fhce.sistemacursos.modules.reporte.dto.ReporteAcademicoDto;
import bo.edu.umsa.fhce.sistemacursos.modules.reporte.dto.ReporteAcademicoEventoDto;
import bo.edu.umsa.fhce.sistemacursos.modules.reporte.dto.ReporteFinancieroActividadDto;
import bo.edu.umsa.fhce.sistemacursos.modules.reporte.dto.ReporteFinancieroDto;
import bo.edu.umsa.fhce.sistemacursos.modules.reporte.dto.ReporteParticipacionCarreraDto;
import bo.edu.umsa.fhce.sistemacursos.modules.reporte.dto.ReporteParticipacionDto;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Usuario;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.repository.UsuarioRepository;
import bo.edu.umsa.fhce.sistemacursos.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReporteService {

    @Value("${app.reportes.logo:}")
    private String reporteLogoPath;

    private static final DateTimeFormatter FECHA_FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA_FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CursoRepository cursoRepository;
    private final EventoRepository eventoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final CoordinadorCarreraRepository coordinadorCarreraRepository;
    private final UsuarioRepository usuarioRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final ParaleloRepository paraleloRepository;

    @Transactional(readOnly = true)
    public ReporteAcademicoDto reporteAcademico(Long idCarrera, LocalDate desde, LocalDate hasta) {
        Filtros filtros = construirFiltros(idCarrera);

        if (filtros.carrerasPermitidas() != null && filtros.carrerasPermitidas().isEmpty()) {
            ReporteAcademicoDto dto = new ReporteAcademicoDto();
            dto.setCursos(List.of());
            dto.setEventos(List.of());
            return dto;
        }

        List<ReporteAcademicoCursoDto> cursos = cursoRepository.reporteAcademicoCursos(
            filtros.idCarrera(), filtros.carrerasPermitidas(), desde, hasta);

        List<ReporteAcademicoEventoDto> eventos = eventoRepository.reporteAcademicoEventos(
            filtros.idCarrera(), filtros.carrerasPermitidas(),
            toInicioDia(desde), toFinDia(hasta));

        ReporteAcademicoDto dto = new ReporteAcademicoDto();
        dto.setCursos(cursos);
        dto.setEventos(eventos);
        return dto;
    }

    @Transactional(readOnly = true)
    public ReporteFinancieroDto reporteFinanciero(Long idCarrera, LocalDate desde, LocalDate hasta) {
        Filtros filtros = construirFiltrosAdmin(idCarrera);
        Inscripcion.TipoPrecio umsa = Inscripcion.TipoPrecio.UMSA;
        Inscripcion.TipoPrecio externo = Inscripcion.TipoPrecio.EXTERNO;

        LocalDateTime desdeDt = toInicioDia(desde);
        LocalDateTime hastaDt = toFinDia(hasta);

        List<ReporteFinancieroActividadDto> cursos = inscripcionRepository
            .reporteFinancieroCursos(filtros.idCarrera(), filtros.carrerasPermitidas(),
                desdeDt, hastaDt, umsa, externo);

        List<ReporteFinancieroActividadDto> eventos = inscripcionRepository
            .reporteFinancieroEventos(filtros.idCarrera(), filtros.carrerasPermitidas(),
                desdeDt, hastaDt, umsa, externo);

        ReporteFinancieroDto dto = new ReporteFinancieroDto();
        dto.setCursos(cursos);
        dto.setEventos(eventos);

        BigDecimal totalUmsa = sumarIngresos(cursos, eventos, true);
        BigDecimal totalExterno = sumarIngresos(cursos, eventos, false);
        dto.setTotalUmsa(totalUmsa);
        dto.setTotalExterno(totalExterno);
        dto.setTotalGeneral(totalUmsa.add(totalExterno));
        return dto;
    }

    @Transactional(readOnly = true)
    public ReporteFinancieroDto reporteFinancieroCoordinador(Long idCarrera, LocalDate desde, LocalDate hasta) {
        Filtros filtros = construirFiltros(idCarrera);

        if (filtros.carrerasPermitidas() != null && filtros.carrerasPermitidas().isEmpty()) {
            ReporteFinancieroDto dto = new ReporteFinancieroDto();
            dto.setCursos(List.of());
            dto.setEventos(List.of());
            dto.setTotalUmsa(BigDecimal.ZERO);
            dto.setTotalExterno(BigDecimal.ZERO);
            dto.setTotalGeneral(BigDecimal.ZERO);
            return dto;
        }

        Inscripcion.TipoPrecio umsa = Inscripcion.TipoPrecio.UMSA;
        Inscripcion.TipoPrecio externo = Inscripcion.TipoPrecio.EXTERNO;

        LocalDateTime desdeDt = toInicioDia(desde);
        LocalDateTime hastaDt = toFinDia(hasta);

        List<ReporteFinancieroActividadDto> cursos = inscripcionRepository
            .reporteFinancieroCursos(filtros.idCarrera(), filtros.carrerasPermitidas(),
                desdeDt, hastaDt, umsa, externo);

        List<ReporteFinancieroActividadDto> eventos = inscripcionRepository
            .reporteFinancieroEventos(filtros.idCarrera(), filtros.carrerasPermitidas(),
                desdeDt, hastaDt, umsa, externo);

        ReporteFinancieroDto dto = new ReporteFinancieroDto();
        dto.setCursos(cursos);
        dto.setEventos(eventos);

        BigDecimal totalUmsa = sumarIngresos(cursos, eventos, true);
        BigDecimal totalExterno = sumarIngresos(cursos, eventos, false);
        dto.setTotalUmsa(totalUmsa);
        dto.setTotalExterno(totalExterno);
        dto.setTotalGeneral(totalUmsa.add(totalExterno));
        return dto;
    }

    @Transactional(readOnly = true)
    public ReporteParticipacionDto reporteParticipacion(Long idCarrera, LocalDate desde, LocalDate hasta) {
        Filtros filtros = construirFiltros(idCarrera);

        if (filtros.carrerasPermitidas() != null && filtros.carrerasPermitidas().isEmpty()) {
            ReporteParticipacionDto dto = new ReporteParticipacionDto();
            dto.setCarreras(List.of());
            dto.setTotalUmsa(0);
            dto.setTotalExterno(0);
            dto.setTotalGeneral(0);
            return dto;
        }
        Inscripcion.TipoPrecio umsa = Inscripcion.TipoPrecio.UMSA;
        Inscripcion.TipoPrecio externo = Inscripcion.TipoPrecio.EXTERNO;

        LocalDateTime desdeDt = toInicioDia(desde);
        LocalDateTime hastaDt = toFinDia(hasta);

        List<ReporteParticipacionCarreraDto> cursos = inscripcionRepository
            .reporteParticipacionPorCarreraCursos(
                filtros.idCarrera(), filtros.carrerasPermitidas(),
                desdeDt, hastaDt, umsa, externo);

        List<ReporteParticipacionCarreraDto> eventos = inscripcionRepository
            .reporteParticipacionPorCarreraEventos(
                filtros.idCarrera(), filtros.carrerasPermitidas(),
                desdeDt, hastaDt, umsa, externo);

        Map<Long, ReporteParticipacionCarreraDto> merged = new HashMap<>();
        mergeParticipacion(merged, cursos);
        mergeParticipacion(merged, eventos);

        List<ReporteParticipacionCarreraDto> carreras = new ArrayList<>(merged.values());
        carreras.sort((a, b) -> a.getCarrera().compareToIgnoreCase(b.getCarrera()));

        ReporteParticipacionDto dto = new ReporteParticipacionDto();
        dto.setCarreras(carreras);

        long totalUmsa = carreras.stream().mapToLong(ReporteParticipacionCarreraDto::getParticipantesUmsa).sum();
        long totalExterno = carreras.stream().mapToLong(ReporteParticipacionCarreraDto::getParticipantesExterno).sum();
        dto.setTotalUmsa(totalUmsa);
        dto.setTotalExterno(totalExterno);
        dto.setTotalGeneral(totalUmsa + totalExterno);
        return dto;
    }

    @Transactional(readOnly = true)
    public ReporteActividadDetalleDto reporteDetalleActividad(String tipo, Long idActividad) {
        if (tipo == null || idActividad == null) {
            throw new BusinessException("Tipo e idActividad son requeridos", 400);
        }

        String tipoNormalizado = tipo.trim().toUpperCase();
        if ("CURSO".equals(tipoNormalizado)) {
            Curso curso = cursoRepository.findById(idActividad)
                .orElseThrow(() -> new ResourceNotFoundException("Curso", idActividad));

            validarAccesoCarrera(curso.getCarrera().getIdCarrera());

            long inscritos = inscripcionRepository.countByCurso_IdCursoAndEstado(
                curso.getIdCurso(), Inscripcion.EstadoInscripcion.CONFIRMADA);
            Integer cupoMaximoRaw = paraleloRepository.sumarCupoMaximo(curso.getIdCurso());
            long cupoMaximo = cupoMaximoRaw != null ? cupoMaximoRaw.longValue() : 0L;
            long cuposDisponibles = Math.max(0, cupoMaximo - inscritos);

            long internos = inscripcionRepository.contarConfirmadasCursoPorTipo(
                curso.getIdCurso(), Inscripcion.TipoPrecio.UMSA);
            long externos = inscripcionRepository.contarConfirmadasCursoPorTipo(
                curso.getIdCurso(), Inscripcion.TipoPrecio.EXTERNO);

            long aprobados = evaluacionRepository.contarAprobadosPorCurso(curso.getIdCurso());

            BigDecimal ingresosUmsa = inscripcionRepository.sumarSaldoCursoPorTipo(
                curso.getIdCurso(), Inscripcion.TipoPrecio.UMSA);
            BigDecimal ingresosExterno = inscripcionRepository.sumarSaldoCursoPorTipo(
                curso.getIdCurso(), Inscripcion.TipoPrecio.EXTERNO);

            ReporteActividadDetalleDto dto = new ReporteActividadDetalleDto();
            dto.setTipo("CURSO");
            dto.setIdActividad(curso.getIdCurso());
            dto.setNombre(curso.getNombre());
            dto.setCarrera(curso.getCarrera().getNombre());
            dto.setEstado(curso.getEstado().name());
            dto.setInscritosConfirmados(inscritos);
            dto.setCupoMaximo(cupoMaximo);
            dto.setCuposDisponibles(cuposDisponibles);
            dto.setParticipantesUmsa(internos);
            dto.setParticipantesExterno(externos);
            dto.setAprobados(aprobados);
            dto.setAsistidos(0L);
            dto.setIngresosUmsa(ingresosUmsa);
            dto.setIngresosExterno(ingresosExterno);
            dto.setIngresosTotal(ingresosUmsa.add(ingresosExterno));
            return dto;
        }

        if ("EVENTO".equals(tipoNormalizado)) {
            Evento evento = eventoRepository.findById(idActividad)
                .orElseThrow(() -> new ResourceNotFoundException("Evento", idActividad));

            validarAccesoCarrera(evento.getCarrera().getIdCarrera());

            long inscritos = inscripcionRepository.countByEvento_IdEventoAndEstado(
                evento.getIdEvento(), Inscripcion.EstadoInscripcion.CONFIRMADA);
            long cupoMaximo = evento.getCupoMaximo() != null ? evento.getCupoMaximo().longValue() : 0L;
            long cuposDisponibles = Math.max(0, cupoMaximo - inscritos);

            long internos = inscripcionRepository.contarConfirmadasEventoPorTipo(
                evento.getIdEvento(), Inscripcion.TipoPrecio.UMSA);
            long externos = inscripcionRepository.contarConfirmadasEventoPorTipo(
                evento.getIdEvento(), Inscripcion.TipoPrecio.EXTERNO);

            long asistidos = asistenciaRepository.countByInscripcion_Evento_IdEvento(evento.getIdEvento());

            BigDecimal ingresosUmsa = inscripcionRepository.sumarSaldoEventoPorTipo(
                evento.getIdEvento(), Inscripcion.TipoPrecio.UMSA);
            BigDecimal ingresosExterno = inscripcionRepository.sumarSaldoEventoPorTipo(
                evento.getIdEvento(), Inscripcion.TipoPrecio.EXTERNO);

            ReporteActividadDetalleDto dto = new ReporteActividadDetalleDto();
            dto.setTipo("EVENTO");
            dto.setIdActividad(evento.getIdEvento());
            dto.setNombre(evento.getNombre());
            dto.setCarrera(evento.getCarrera().getNombre());
            dto.setEstado(evento.getEstado().name());
            dto.setInscritosConfirmados(inscritos);
            dto.setCupoMaximo(cupoMaximo);
            dto.setCuposDisponibles(cuposDisponibles);
            dto.setParticipantesUmsa(internos);
            dto.setParticipantesExterno(externos);
            dto.setAprobados(0L);
            dto.setAsistidos(asistidos);
            dto.setIngresosUmsa(ingresosUmsa);
            dto.setIngresosExterno(ingresosExterno);
            dto.setIngresosTotal(ingresosUmsa.add(ingresosExterno));
            return dto;
        }

        throw new BusinessException("Tipo de actividad no valido", 400);
    }

    @Transactional(readOnly = true)
    public byte[] reporteInscritosPdf(String tipo, Long idActividad) {
        if (tipo == null || idActividad == null) {
            throw new BusinessException("Tipo e idActividad son requeridos", 400);
        }
    
        String tipoNormalizado = tipo.trim().toUpperCase(Locale.ROOT);
    
        String nombreActividad;
        String fechaActividad;
        String lugarActividad;
        String carrera;
        long inscritosConfirmados;
        long cupoMaximo;
        List<Inscripcion> inscritos;
    
        if ("CURSO".equals(tipoNormalizado)) {
            Curso curso = cursoRepository.findById(idActividad)
                .orElseThrow(() -> new ResourceNotFoundException("Curso", idActividad));
            validarAccesoCarrera(curso.getCarrera().getIdCarrera());
            nombreActividad = curso.getNombre();
            fechaActividad = curso.getFechaInicio() != null ? curso.getFechaInicio().format(FECHA_FORMATO) : "-";
            lugarActividad = "-";
            carrera = curso.getCarrera().getNombre();
            inscritos = inscripcionRepository.findConfirmadasCursoParaReporte(curso.getIdCurso());
            inscritosConfirmados = inscritos.size();
            Integer cupoMaximoRaw = paraleloRepository.sumarCupoMaximo(curso.getIdCurso());
            cupoMaximo = cupoMaximoRaw != null ? cupoMaximoRaw.longValue() : 0L;
        } else if ("EVENTO".equals(tipoNormalizado)) {
            Evento evento = eventoRepository.findById(idActividad)
                .orElseThrow(() -> new ResourceNotFoundException("Evento", idActividad));
            validarAccesoCarrera(evento.getCarrera().getIdCarrera());
            nombreActividad = evento.getNombre();
            fechaActividad = evento.getFechaHora() != null ? evento.getFechaHora().format(FECHA_HORA_FORMATO) : "-";
            lugarActividad = (evento.getLugar() == null || evento.getLugar().isBlank()) ? "-" : evento.getLugar();
            carrera = evento.getCarrera().getNombre();
            inscritos = inscripcionRepository.findConfirmadasEventoParaReporte(evento.getIdEvento());
            inscritosConfirmados = inscritos.size();
            cupoMaximo = evento.getCupoMaximo() != null ? evento.getCupoMaximo().longValue() : 0L;
        } else {
            throw new BusinessException("Tipo de actividad no valido", 400);
        }
    
        // ── Colores institucionales ──────────────────────────────────────────────
        DeviceRgb  AZUL_OSCURO    = new DeviceRgb(0x1a, 0x3a, 0x5c);   // #1a3a5c
        DeviceRgb  AZUL_MEDIO     = new DeviceRgb(0x25, 0x5e, 0x9e);   // #255e9e
        DeviceRgb  AZUL_CLARO     = new DeviceRgb(0xe8, 0xf1, 0xfb);   // #e8f1fb
        DeviceRgb  GRIS_CABECERA  = new DeviceRgb(0xf2, 0xf4, 0xf7);   // #f2f4f7
        DeviceRgb  GRIS_BORDE     = new DeviceRgb(0xcc, 0xd5, 0xe0);   // #ccd5e0
        DeviceRgb  BLANCO         = new DeviceRgb(255, 255, 255);
    
        String cupoTexto = cupoMaximo > 0
            ? inscritosConfirmados + " / " + cupoMaximo + " cupos"
            : inscritosConfirmados + " inscritos";
    
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(output);
            PdfDocument pdfDoc = new PdfDocument(writer);
    
            // ── Footer con número de página ──────────────────────────────────────
            final String footerLabel = nombreActividad;
            pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE,
                new AbstractPdfDocumentEventHandler() {
                    @Override
                    protected void onAcceptedEvent(AbstractPdfDocumentEvent event) {
                        try {
                            PdfDocumentEvent ev = (PdfDocumentEvent) event;
                            PdfPage page = ev.getPage();
                            PdfCanvas canvas = new PdfCanvas(page);
                            PdfFont fNormal = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                            int pageNum = pdfDoc.getPageNumber(page);
                            int total   = pdfDoc.getNumberOfPages();
                            float pageWidth = page.getPageSize().getWidth();
                            float y = 20f;

                            canvas.setStrokeColor(new DeviceRgb(0xcc, 0xd5, 0xe0))
                                .setLineWidth(0.5f)
                                .moveTo(36, y + 12)
                                .lineTo(pageWidth - 36, y + 12)
                                .stroke();

                            canvas.beginText()
                                .setFontAndSize(fNormal, 8)
                                .setColor(new DeviceRgb(0x88, 0x88, 0x88), true)
                                .moveText(36, y)
                                .showText("Reporte de Inscritos \u2014 " + footerLabel)
                                .endText();

                            String pageText = "P\u00e1gina " + pageNum + " de " + total;
                            float tw = fNormal.getWidth(pageText, 8);
                            canvas.beginText()
                                .setFontAndSize(fNormal, 8)
                                .setColor(new DeviceRgb(0x88, 0x88, 0x88), true)
                                .moveText(pageWidth - 36 - tw, y)
                                .showText(pageText)
                                .endText();

                            canvas.release();
                        } catch (Exception ignored) {}
                    }
                }
            );
                
            Document document = new Document(pdfDoc, PageSize.LETTER);
            document.setMargins(36, 36, 52, 36);
    
            PdfFont fontNormal  = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont fontBold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont fontOblique = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);
    
            // ── HEADER ───────────────────────────────────────────────────────────
            // Franja azul superior
            Table headerBand = new Table(UnitValue.createPercentArray(new float[]{15, 70, 15}))
                .useAllAvailableWidth()
                .setMarginBottom(0);
    
            // Celda logo (izquierda)
            Cell logoCell = new Cell().setBorder(Border.NO_BORDER)
                .setBackgroundColor(AZUL_OSCURO)
                .setPadding(10)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    
            boolean logoAgregado = false;
            if (reporteLogoPath != null && !reporteLogoPath.isBlank()) {
                try {
                    java.nio.file.Path lp = java.nio.file.Paths.get(reporteLogoPath);
                    if (java.nio.file.Files.exists(lp)) {
                        Image logo = new Image(ImageDataFactory.create(reporteLogoPath))
                            .setWidth(45).setHeight(45)
                            .setHorizontalAlignment(HorizontalAlignment.CENTER);
                        logoCell.add(logo);
                        logoAgregado = true;
                    }
                } catch (Exception ignored) {}
            }
            if (!logoAgregado) {
                logoCell.add(new Paragraph("FHCE")
                    .setFont(fontBold).setFontSize(13)
                    .setFontColor(BLANCO)
                    .setTextAlignment(TextAlignment.CENTER));
            }
            headerBand.addCell(logoCell);
    
            // Celda título (centro)
            headerBand.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(AZUL_OSCURO)
                .setPadding(10)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(new Paragraph("REPORTE DE INSCRITOS")
                    .setFont(fontBold).setFontSize(16)
                    .setFontColor(BLANCO)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setCharacterSpacing(1f))
                .add(new Paragraph(tipoNormalizado.equals("CURSO") ? "Curso" : "Evento")
                    .setFont(fontOblique).setFontSize(10)
                    .setFontColor(new DeviceRgb(0xaa, 0xcc, 0xee))
                    .setTextAlignment(TextAlignment.CENTER)));
    
            // Celda fecha (derecha)
            headerBand.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(AZUL_OSCURO)
                .setPadding(10)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(new Paragraph(java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy\nHH:mm")))
                    .setFont(fontNormal).setFontSize(9)
                    .setFontColor(new DeviceRgb(0xaa, 0xcc, 0xee))
                    .setTextAlignment(TextAlignment.RIGHT)));
    
            document.add(headerBand);
    
            // Franja delgada de acento
            Table accentBar = new Table(1).useAllAvailableWidth().setMarginBottom(14);
            accentBar.addCell(new Cell()
                .setBackgroundColor(AZUL_MEDIO)
                .setHeight(4)
                .setBorder(Border.NO_BORDER));
            document.add(accentBar);
    
            // ── FICHA DE INFORMACIÓN ─────────────────────────────────────────────
            // Tarjeta gris de datos de la actividad
            Table infoCard = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth()
                .setBackgroundColor(GRIS_CABECERA)
                .setBorder(new SolidBorder(GRIS_BORDE, 0.5f))
                .setBorderRadius(new BorderRadius(4))
                .setMarginBottom(18);
    
            // Fila 1
            infoCard.addCell(infoLabel("Actividad", fontBold, AZUL_MEDIO));
            infoCard.addCell(infoLabel("Carrera", fontBold, AZUL_MEDIO));
            infoCard.addCell(infoValue(nombreActividad, fontNormal));
            infoCard.addCell(infoValue(carrera, fontNormal));
    
            // Fila 2
            infoCard.addCell(infoLabel("Fecha", fontBold, AZUL_MEDIO));
            infoCard.addCell(infoLabel("Lugar", fontBold, AZUL_MEDIO));
            infoCard.addCell(infoValue(fechaActividad, fontNormal));
            infoCard.addCell(infoValue(lugarActividad, fontNormal));
    
            // Fila 3 — inscritos (span completo)
            infoCard.addCell(infoLabel("Inscritos confirmados", fontBold, AZUL_MEDIO));
            infoCard.addCell(infoLabel(" ", fontBold, AZUL_MEDIO));  // placeholder
            infoCard.addCell(new Cell(1, 2)
                .setBorder(Border.NO_BORDER)
                .setPaddingLeft(12).setPaddingBottom(10)
                .add(new Paragraph(cupoTexto)
                    .setFont(fontBold).setFontSize(11)
                    .setFontColor(AZUL_OSCURO)));
    
            document.add(infoCard);
    
            // ── TABLA(S) DE INSCRITOS ────────────────────────────────────────────
            if ("CURSO".equals(tipoNormalizado)) {
                java.util.Map<String, java.util.List<Inscripcion>> porParalelo = new java.util.LinkedHashMap<>();
                java.util.List<bo.edu.umsa.fhce.sistemacursos.modules.curso.entity.Paralelo> paralelos =
                    paraleloRepository.findById_IdCurso(idActividad);
                for (bo.edu.umsa.fhce.sistemacursos.modules.curso.entity.Paralelo p : paralelos) {
                    porParalelo.put(p.getId().getCodigo(), new java.util.ArrayList<>());
                }
                for (Inscripcion ins : inscritos) {
                    String codigo = ins.getCodigoParalelo();
                    if (codigo == null) {
                        codigo = "-";
                        porParalelo.putIfAbsent("-", new java.util.ArrayList<>());
                    }
                    porParalelo.get(codigo).add(ins);
                }
    
                for (java.util.Map.Entry<String, java.util.List<Inscripcion>> entry : porParalelo.entrySet()) {
                    String codigo = entry.getKey();
                    java.util.List<Inscripcion> lista = entry.getValue();
                    if (lista.isEmpty()) continue;
    
                    Integer cupoParalelo = null;
                    if (!"-".equals(codigo)) {
                        try {
                            bo.edu.umsa.fhce.sistemacursos.modules.curso.entity.Paralelo p =
                                paraleloRepository.findById(
                                    new bo.edu.umsa.fhce.sistemacursos.modules.curso.entity.ParaleloId(idActividad, codigo))
                                .orElse(null);
                            if (p != null) cupoParalelo = p.getCupoMaximo();
                        } catch (Exception ignored) {}
                    }
    
                    // Cabecera de paralelo con banda de color
                    String labelParalelo = (!"-".equals(codigo) ? "Paralelo: " + codigo : "Sin paralelo")
                        + "  |  " + lista.size() + " inscritos"
                        + (cupoParalelo != null ? " / " + cupoParalelo + " cupos" : "");
    
                    Table secHeader = new Table(1).useAllAvailableWidth().setMarginBottom(0).setMarginTop(10);
                    secHeader.addCell(new Cell()
                        .setBackgroundColor(AZUL_MEDIO)
                        .setBorder(Border.NO_BORDER)
                        .setPadding(6)
                        .add(new Paragraph(labelParalelo)
                            .setFont(fontBold).setFontSize(10)
                            .setFontColor(BLANCO)));
                    document.add(secHeader);
    
                    document.add(buildInscritosTable(lista, fontBold, fontNormal,
                        AZUL_OSCURO, AZUL_CLARO, GRIS_BORDE, BLANCO));
                }
    
            } else {
                // Evento: tabla directa
                Table secHeader = new Table(1).useAllAvailableWidth().setMarginBottom(0).setMarginTop(4);
                secHeader.addCell(new Cell()
                    .setBackgroundColor(AZUL_MEDIO)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(6)
                    .add(new Paragraph("Participantes — " + inscritos.size() + " inscritos confirmados")
                        .setFont(fontBold).setFontSize(10)
                        .setFontColor(BLANCO)));
                document.add(secHeader);
    
                document.add(buildInscritosTable(inscritos, fontBold, fontNormal,
                    AZUL_OSCURO, AZUL_CLARO, GRIS_BORDE, BLANCO));
            }
    
            document.close();
            return output.toByteArray();
    
        } catch (IOException e) {
            throw new RuntimeException("No se pudo generar el PDF de inscritos", e);
        }
    }
    private Table buildInscritosTable(
            List<Inscripcion> lista,
            PdfFont fontBold, PdfFont fontNormal,
            DeviceRgb  headerBg, DeviceRgb  stripeBg, DeviceRgb  borderColor, DeviceRgb  headerFg) {
    
        Table table = new Table(UnitValue.createPercentArray(new float[]{7, 22, 22, 16, 33}))
            .useAllAvailableWidth()
            .setBorder(new SolidBorder(borderColor, 0.5f))
            .setMarginBottom(6);
    
        // Cabecera
        for (String col : new String[]{"Nro", "Nombres", "Apellidos", "CI", "Correo"}) {
            table.addHeaderCell(new Cell()
                .setBackgroundColor(headerBg)
                .setBorder(new SolidBorder(borderColor, 0.3f))
                .setPaddingTop(7).setPaddingBottom(7).setPaddingLeft(6).setPaddingRight(6)
                .add(new Paragraph(col)
                    .setFont(fontBold).setFontSize(9)
                    .setFontColor(headerFg)
                    .setTextAlignment(TextAlignment.CENTER)));
        }
    
        if (lista.isEmpty()) {
            table.addCell(new Cell(1, 5)
                .setBorder(new SolidBorder(borderColor, 0.3f))
                .setPadding(10)
                .setTextAlignment(TextAlignment.CENTER)
                .add(new Paragraph("No hay inscritos confirmados.")
                    .setFont(fontNormal).setFontSize(10)
                    .setFontColor(ColorConstants.GRAY)));
            return table;
        }
    
        int idx = 1;
        for (Inscripcion ins : lista) {
            Usuario p = ins.getParticipante();
            boolean par = idx % 2 == 0;
            DeviceRgb rowBg = par ? stripeBg : new DeviceRgb(255, 255, 255);
    
            table.addCell(styledCell(String.valueOf(idx), TextAlignment.CENTER, fontNormal, rowBg, borderColor));
            table.addCell(styledCell(orDash(p.getNombres()),   TextAlignment.LEFT, fontNormal, rowBg, borderColor));
            table.addCell(styledCell(orDash(p.getApellidos()), TextAlignment.LEFT, fontNormal, rowBg, borderColor));
            table.addCell(styledCell(orDash(p.getCi()),        TextAlignment.LEFT, fontNormal, rowBg, borderColor));
            table.addCell(styledCell(orDash(p.getEmail()),     TextAlignment.LEFT, fontNormal, rowBg, borderColor));
            idx++;
        }
        return table;
    }
    
    private Cell styledCell(String text, TextAlignment align, PdfFont font, DeviceRgb  bg, DeviceRgb  border) {
        return new Cell()
            .setBackgroundColor(bg)
            .setBorder(new SolidBorder(border, 0.3f))
            .setPaddingTop(5).setPaddingBottom(5).setPaddingLeft(6).setPaddingRight(6)
            .add(new Paragraph(text).setFont(font).setFontSize(9).setTextAlignment(align));
    }
    
    private Cell infoLabel(String text, PdfFont fontBold, DeviceRgb  color) {
        return new Cell()
            .setBorder(Border.NO_BORDER)
            .setPaddingLeft(12).setPaddingTop(8).setPaddingBottom(2)
            .add(new Paragraph(text.toUpperCase())
                .setFont(fontBold).setFontSize(7.5f)
                .setFontColor(color).setCharacterSpacing(0.5f));
    }
    
    private Cell infoValue(String text, PdfFont fontNormal) {
        return new Cell()
            .setBorder(Border.NO_BORDER)
            .setPaddingLeft(12).setPaddingBottom(8)
            .add(new Paragraph(text)
                .setFont(fontNormal).setFontSize(10));
    }
    private Cell createHeaderCell(String text, PdfFont fontBold) {
        return new Cell()
            .add(new Paragraph(text).setFont(fontBold).setFontSize(10))
            .setBackgroundColor(ColorConstants.LIGHT_GRAY)
            .setPadding(6)
            .setTextAlignment(TextAlignment.CENTER);
    }

    private Cell createBodyCell(String text, TextAlignment alignment, PdfFont fontNormal) {
        return new Cell()
            .add(new Paragraph(text).setFont(fontNormal).setFontSize(10))
            .setPadding(5)
            .setTextAlignment(alignment);
    }

    private String orDash(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }

    private void mergeParticipacion(
            Map<Long, ReporteParticipacionCarreraDto> merged,
            List<ReporteParticipacionCarreraDto> rows) {
        for (ReporteParticipacionCarreraDto row : rows) {
            ReporteParticipacionCarreraDto actual = merged.get(row.getIdCarrera());
            if (actual == null) {
                merged.put(row.getIdCarrera(), row);
            } else {
                actual.setParticipantesUmsa(actual.getParticipantesUmsa() + row.getParticipantesUmsa());
                actual.setParticipantesExterno(actual.getParticipantesExterno() + row.getParticipantesExterno());
                actual.setTotal(actual.getParticipantesUmsa() + actual.getParticipantesExterno());
            }
        }
    }

    private BigDecimal sumarIngresos(
            List<ReporteFinancieroActividadDto> cursos,
            List<ReporteFinancieroActividadDto> eventos,
            boolean umsa) {
        BigDecimal total = BigDecimal.ZERO;
        for (ReporteFinancieroActividadDto row : cursos) {
            total = total.add(umsa ? row.getIngresosUmsa() : row.getIngresosExterno());
        }
        for (ReporteFinancieroActividadDto row : eventos) {
            total = total.add(umsa ? row.getIngresosUmsa() : row.getIngresosExterno());
        }
        return total;
    }

    private Filtros construirFiltros(Long idCarrera) {
        Usuario usuario = getUsuarioActual();
        boolean esAdmin = tieneRol(usuario, "ADMINISTRADOR");
        boolean esCoordinador = tieneRol(usuario, "COORDINADOR");

        if (!esAdmin && !esCoordinador) {
            throw new BusinessException("No tienes permisos para acceder a reportes", 403);
        }

        if (esAdmin) {
            return new Filtros(idCarrera, null);
        }

        List<Long> carreras = coordinadorCarreraRepository
            .findByIdCoordinador(usuario.getIdUsuario())
            .stream()
            .map(cc -> cc.getCarrera().getIdCarrera())
            .toList();

        if (idCarrera != null && !carreras.contains(idCarrera)) {
            throw new BusinessException("No tienes permisos para esta carrera", 403);
        }

        if (idCarrera != null) {
            return new Filtros(idCarrera, List.of(idCarrera));
        }

        return new Filtros(null, carreras);
    }

    private Filtros construirFiltrosAdmin(Long idCarrera) {
        Usuario usuario = getUsuarioActual();
        boolean esAdmin = tieneRol(usuario, "ADMINISTRADOR");
        if (!esAdmin) {
            throw new BusinessException("Solo el administrador puede acceder a reportes financieros", 403);
        }
        return new Filtros(idCarrera, null);
    }

    private void validarAccesoCarrera(Long idCarrera) {
        construirFiltros(idCarrera);
    }

    private boolean tieneRol(Usuario usuario, String rol) {
        return usuario.getRoles().stream().anyMatch(r -> r.getNombre().equals(rol));
    }

    private Usuario getUsuarioActual() {
        CustomUserDetails userDetails = (CustomUserDetails)
            SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return usuarioRepository.findById(userDetails.getIdUsuario())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Usuario", userDetails.getIdUsuario()));
    }

    private LocalDateTime toInicioDia(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime toFinDia(LocalDate date) {
        return date == null ? null : date.atTime(LocalTime.MAX);
    }

    @Transactional(readOnly = true)
    public byte[] reporteAcademicoCoordinadorPdf(LocalDate desde, LocalDate hasta,
            Long idCarrera, String tipo, String buscar) {

        Filtros filtros = construirFiltros(idCarrera);

        ReporteAcademicoDto dto = reporteAcademico(idCarrera, desde, hasta);
        List<ReporteAcademicoCursoDto> cursos = dto.getCursos() != null ? dto.getCursos() : List.of();
        List<ReporteAcademicoEventoDto> eventos = dto.getEventos() != null ? dto.getEventos() : List.of();

        DeviceRgb  AZUL_OSCURO    = new DeviceRgb(0x1a, 0x3a, 0x5c);
        DeviceRgb  AZUL_MEDIO     = new DeviceRgb(0x25, 0x5e, 0x9e);
        DeviceRgb  AZUL_CLARO     = new DeviceRgb(0xe8, 0xf1, 0xfb);
        DeviceRgb  GRIS_CABECERA  = new DeviceRgb(0xf2, 0xf4, 0xf7);
        DeviceRgb  GRIS_BORDE     = new DeviceRgb(0xcc, 0xd5, 0xe0);
        DeviceRgb  BLANCO         = new DeviceRgb(255, 255, 255);

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(output);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.LETTER);
            document.setMargins(36, 36, 52, 36);

            PdfFont fontNormal  = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont fontBold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            // Header similar to inscritos
            Table headerBand = new Table(UnitValue.createPercentArray(new float[]{15, 70, 15}))
                .useAllAvailableWidth()
                .setMarginBottom(0);

            Cell logoCell = new Cell().setBorder(Border.NO_BORDER)
                .setBackgroundColor(AZUL_OSCURO)
                .setPadding(10)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

            boolean logoAgregado = false;
            if (reporteLogoPath != null && !reporteLogoPath.isBlank()) {
                try {
                    java.nio.file.Path lp = java.nio.file.Paths.get(reporteLogoPath);
                    if (java.nio.file.Files.exists(lp)) {
                        Image logo = new Image(ImageDataFactory.create(reporteLogoPath))
                            .setWidth(45).setHeight(45)
                            .setHorizontalAlignment(HorizontalAlignment.CENTER);
                        logoCell.add(logo);
                        logoAgregado = true;
                    }
                } catch (Exception ignored) {}
            }
            if (!logoAgregado) {
                logoCell.add(new Paragraph("FHCE").setFont(fontBold).setFontSize(13).setFontColor(BLANCO).setTextAlignment(TextAlignment.CENTER));
            }
            headerBand.addCell(logoCell);

            headerBand.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(AZUL_OSCURO)
                .setPadding(10)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(new Paragraph("REPORTE ACADÉMICO")
                    .setFont(fontBold).setFontSize(16).setFontColor(BLANCO).setTextAlignment(TextAlignment.CENTER)));

            headerBand.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(AZUL_OSCURO)
                .setPadding(10)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(new Paragraph(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy\nHH:mm")))
                    .setFont(fontNormal).setFontSize(9).setFontColor(new DeviceRgb(0xaa,0xcc,0xee)).setTextAlignment(TextAlignment.RIGHT)));

            document.add(headerBand);

            Table accentBar = new Table(1).useAllAvailableWidth().setMarginBottom(12);
            accentBar.addCell(new Cell().setBackgroundColor(AZUL_MEDIO).setHeight(4).setBorder(Border.NO_BORDER));
            document.add(accentBar);

            // Info card with filters
            Table infoCard = new Table(UnitValue.createPercentArray(new float[]{50,50})).useAllAvailableWidth()
                .setBackgroundColor(GRIS_CABECERA).setBorder(new SolidBorder(GRIS_BORDE, 0.5f)).setBorderRadius(new BorderRadius(4)).setMarginBottom(12);
            infoCard.addCell(infoLabel("Periodo", fontBold, AZUL_MEDIO));
            infoCard.addCell(infoLabel("Carrera / Filtro", fontBold, AZUL_MEDIO));
            String periodo = (desde == null ? "-" : desde.format(FECHA_FORMATO)) + " — " + (hasta == null ? "-" : hasta.format(FECHA_FORMATO));
            infoCard.addCell(infoValue(periodo, fontNormal));
            String carreraTxt = idCarrera == null ? "Todas" : (coordinadorCarreraRepository.findByIdCoordinador(getUsuarioActual().getIdUsuario()).stream().filter(cc->cc.getCarrera().getIdCarrera().equals(idCarrera)).findFirst().map(cc->cc.getCarrera().getNombre()).orElse("-"));
            String extras = (tipo == null || tipo.isBlank() ? "" : "Tipo: " + tipo) + (buscar == null || buscar.isBlank() ? "" : " — Buscar: " + buscar);
            infoCard.addCell(infoValue(carreraTxt + (extras.isBlank() ? "" : " | " + extras), fontNormal));
            document.add(infoCard);

            // Table
            Table table = new Table(UnitValue.createPercentArray(new float[]{12, 30, 18, 18, 12, 10}))
                .useAllAvailableWidth().setMarginBottom(8).setBorder(new SolidBorder(GRIS_BORDE, 0.5f));
            for (String h : new String[]{"Tipo","Actividad","Carrera","Fecha","Estado","Inscritos/Cupo"}) {
                table.addHeaderCell(createHeaderCell(h, fontBold));
            }

            int index = 1;
            for (ReporteAcademicoCursoDto c : cursos) {
                if (tipo != null && !tipo.isBlank() && !"CURSO".equals(tipo)) continue;
                if (buscar != null && !buscar.isBlank() && !c.getNombre().toLowerCase().contains(buscar.toLowerCase())) continue;
                table.addCell(createBodyCell("CURSO", TextAlignment.CENTER, fontNormal));
                table.addCell(createBodyCell(orDash(c.getNombre()), TextAlignment.LEFT, fontNormal));
                table.addCell(createBodyCell(orDash(c.getCarrera()), TextAlignment.LEFT, fontNormal));
                table.addCell(createBodyCell(c.getFechaInicio() != null ? c.getFechaInicio().format(FECHA_FORMATO) : "-", TextAlignment.LEFT, fontNormal));
                table.addCell(createBodyCell(c.getEstado() != null ? c.getEstado().name() : "-", TextAlignment.LEFT, fontNormal));
                table.addCell(createBodyCell((c.getInscritosConfirmados() != null ? c.getInscritosConfirmados() : 0) + " / " + (c.getCupoMaximo() != null ? c.getCupoMaximo() : "-"), TextAlignment.RIGHT, fontNormal));
                index++;
            }
            for (ReporteAcademicoEventoDto e : eventos) {
                if (tipo != null && !tipo.isBlank() && !"EVENTO".equals(tipo)) continue;
                if (buscar != null && !buscar.isBlank() && !e.getNombre().toLowerCase().contains(buscar.toLowerCase())) continue;
                table.addCell(createBodyCell("EVENTO", TextAlignment.CENTER, fontNormal));
                table.addCell(createBodyCell(orDash(e.getNombre()), TextAlignment.LEFT, fontNormal));
                table.addCell(createBodyCell(orDash(e.getCarrera()), TextAlignment.LEFT, fontNormal));
                table.addCell(createBodyCell(e.getFechaHora() != null ? e.getFechaHora().format(FECHA_HORA_FORMATO) : "-", TextAlignment.LEFT, fontNormal));
                table.addCell(createBodyCell(e.getEstado() != null ? e.getEstado().name() : "-", TextAlignment.LEFT, fontNormal));
                table.addCell(createBodyCell((e.getInscritosConfirmados() != null ? e.getInscritosConfirmados() : 0) + " / " + (e.getCupoMaximo() != null ? e.getCupoMaximo() : "-"), TextAlignment.RIGHT, fontNormal));
                index++;
            }

            if ((cursos.isEmpty() && eventos.isEmpty())) {
                table.addCell(new Cell(1,6).setBorder(new SolidBorder(GRIS_BORDE,0.3f)).setPadding(10).setTextAlignment(TextAlignment.CENTER)
                    .add(new Paragraph("No hay actividades en el periodo y filtros seleccionados.").setFont(fontNormal).setFontSize(10).setFontColor(ColorConstants.GRAY)));
            }

            document.add(table);
            document.close();
            return output.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo generar el PDF académico", ex);
        }
    }

    @Transactional(readOnly = true)
    public byte[] reporteFinancieroCoordinadorPdf(LocalDate desde, LocalDate hasta,
            Long idCarrera, String tipo, String buscar) {

        Filtros filtros = construirFiltros(idCarrera);
        ReporteFinancieroDto dto = reporteFinancieroCoordinador(idCarrera, desde, hasta);

        DeviceRgb  AZUL_OSCURO    = new DeviceRgb(0x1a, 0x3a, 0x5c);
        DeviceRgb  AZUL_MEDIO     = new DeviceRgb(0x25, 0x5e, 0x9e);
        DeviceRgb  GRIS_CABECERA  = new DeviceRgb(0xf2, 0xf4, 0xf7);
        DeviceRgb  GRIS_BORDE     = new DeviceRgb(0xcc, 0xd5, 0xe0);
        DeviceRgb  BLANCO         = new DeviceRgb(255, 255, 255);

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(output);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.LETTER);
            document.setMargins(36, 36, 52, 36);

            PdfFont fontNormal  = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont fontBold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            Table headerBand = new Table(UnitValue.createPercentArray(new float[]{15, 70, 15}))
                .useAllAvailableWidth().setMarginBottom(0);

            Cell logoCell = new Cell().setBorder(Border.NO_BORDER)
                .setBackgroundColor(AZUL_OSCURO)
                .setPadding(10).setVerticalAlignment(VerticalAlignment.MIDDLE);
            boolean logoAgregado = false;
            if (reporteLogoPath != null && !reporteLogoPath.isBlank()) {
                try {
                    java.nio.file.Path lp = java.nio.file.Paths.get(reporteLogoPath);
                    if (java.nio.file.Files.exists(lp)) {
                        Image logo = new Image(ImageDataFactory.create(reporteLogoPath)).setWidth(45).setHeight(45).setHorizontalAlignment(HorizontalAlignment.CENTER);
                        logoCell.add(logo); logoAgregado = true;
                    }
                } catch (Exception ignored) {}
            }
            if (!logoAgregado) logoCell.add(new Paragraph("FHCE").setFont(fontBold).setFontSize(13).setFontColor(BLANCO).setTextAlignment(TextAlignment.CENTER));
            headerBand.addCell(logoCell);
            headerBand.addCell(new Cell().setBorder(Border.NO_BORDER).setBackgroundColor(AZUL_OSCURO).setPadding(10).setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(new Paragraph("REPORTE FINANCIERO").setFont(fontBold).setFontSize(16).setFontColor(BLANCO).setTextAlignment(TextAlignment.CENTER)));
            headerBand.addCell(new Cell().setBorder(Border.NO_BORDER).setBackgroundColor(AZUL_OSCURO).setPadding(10).setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(new Paragraph(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy\nHH:mm"))).setFont(fontNormal).setFontSize(9).setFontColor(new DeviceRgb(0xaa,0xcc,0xee)).setTextAlignment(TextAlignment.RIGHT)));
            document.add(headerBand);

            Table accentBar = new Table(1).useAllAvailableWidth().setMarginBottom(12);
            accentBar.addCell(new Cell().setBackgroundColor(AZUL_MEDIO).setHeight(4).setBorder(Border.NO_BORDER));
            document.add(accentBar);

            Table infoCard = new Table(UnitValue.createPercentArray(new float[]{50,50})).useAllAvailableWidth()
                .setBackgroundColor(GRIS_CABECERA).setBorder(new SolidBorder(GRIS_BORDE, 0.5f)).setBorderRadius(new BorderRadius(4)).setMarginBottom(12);
            infoCard.addCell(infoLabel("Periodo", fontBold, AZUL_MEDIO));
            infoCard.addCell(infoLabel("Carrera / Filtros", fontBold, AZUL_MEDIO));
            String periodo = (desde == null ? "-" : desde.format(FECHA_FORMATO)) + " — " + (hasta == null ? "-" : hasta.format(FECHA_FORMATO));
            infoCard.addCell(infoValue(periodo, fontNormal));
            String carreraTxt = idCarrera == null ? "Todas" : (coordinadorCarreraRepository.findByIdCoordinador(getUsuarioActual().getIdUsuario()).stream().filter(cc->cc.getCarrera().getIdCarrera().equals(idCarrera)).findFirst().map(cc->cc.getCarrera().getNombre()).orElse("-"));
            String extras = (tipo == null || tipo.isBlank() ? "" : "Tipo: " + tipo) + (buscar == null || buscar.isBlank() ? "" : " — Buscar: " + buscar);
            infoCard.addCell(infoValue(carreraTxt + (extras.isBlank() ? "" : " | " + extras), fontNormal));
            document.add(infoCard);

            Table table = new Table(UnitValue.createPercentArray(new float[]{12, 30, 18, 12, 12, 16})).useAllAvailableWidth().setMarginBottom(8).setBorder(new SolidBorder(GRIS_BORDE, 0.5f));
            for (String h : new String[]{"Tipo","Actividad","Carrera","UMSA","Externo","Total"}) table.addHeaderCell(createHeaderCell(h, fontBold));

            int index = 1;
            List<ReporteFinancieroActividadDto> rows = new ArrayList<>();
            if (dto.getCursos() != null) rows.addAll(dto.getCursos());
            if (dto.getEventos() != null) rows.addAll(dto.getEventos());

            for (ReporteFinancieroActividadDto r : rows) {
                if (tipo != null && !tipo.isBlank() && (r.getTipo() == null || !r.getTipo().equalsIgnoreCase(tipo))) continue;
                if (buscar != null && !buscar.isBlank() && !r.getNombre().toLowerCase().contains(buscar.toLowerCase())) continue;
                table.addCell(createBodyCell(orDash(r.getTipo()), TextAlignment.CENTER, fontNormal));
                table.addCell(createBodyCell(orDash(r.getNombre()), TextAlignment.LEFT, fontNormal));
                table.addCell(createBodyCell(orDash(r.getCarrera()), TextAlignment.LEFT, fontNormal));
                table.addCell(createBodyCell(r.getIngresosUmsa() != null ? r.getIngresosUmsa().toString() : "0", TextAlignment.RIGHT, fontNormal));
                table.addCell(createBodyCell(r.getIngresosExterno() != null ? r.getIngresosExterno().toString() : "0", TextAlignment.RIGHT, fontNormal));
                table.addCell(createBodyCell(r.getIngresosTotal() != null ? r.getIngresosTotal().toString() : "0", TextAlignment.RIGHT, fontNormal));
                index++;
            }

            if (rows.isEmpty()) {
                table.addCell(new Cell(1,6).setBorder(new SolidBorder(GRIS_BORDE,0.3f)).setPadding(10).setTextAlignment(TextAlignment.CENTER)
                    .add(new Paragraph("No hay ingresos en el periodo y filtros seleccionados.").setFont(fontNormal).setFontSize(10).setFontColor(ColorConstants.GRAY)));
            } else {
                // Totals row
                table.addCell(new Cell(1,3).setBorder(Border.NO_BORDER).setPadding(6).add(new Paragraph("Totales").setFont(fontBold)));
                table.addCell(createBodyCell(dto.getTotalUmsa() != null ? dto.getTotalUmsa().toString() : "0", TextAlignment.RIGHT, fontBold));
                table.addCell(createBodyCell(dto.getTotalExterno() != null ? dto.getTotalExterno().toString() : "0", TextAlignment.RIGHT, fontBold));
                table.addCell(createBodyCell(dto.getTotalGeneral() != null ? dto.getTotalGeneral().toString() : "0", TextAlignment.RIGHT, fontBold));
            }

            document.add(table);
            document.close();
            return output.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo generar el PDF financiero", ex);
        }
    }

    private record Filtros(Long idCarrera, List<Long> carrerasPermitidas) {}
}