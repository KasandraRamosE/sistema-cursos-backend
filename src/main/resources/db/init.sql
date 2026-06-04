-- ============================================================
-- SISTEMA WEB DE GESTIÓN ACADÉMICA - FHCE / UMSA
-- Base de datos: MySQL 8.0+
-- ============================================================

-- Configuración de caracteres
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- A partir de aquí va todo tu script SQL desde USE sistema_cursos_fhce;
-- hasta el final (incluyendo triggers)
USE sistema_cursos_fhce;
-- ============================================================
-- MÓDULO 1: USUARIOS Y AUTENTICACIÓN
-- ============================================================

CREATE TABLE usuario (
    id_usuario        BIGINT          AUTO_INCREMENT PRIMARY KEY,
    username          VARCHAR(50)     NOT NULL UNIQUE  COMMENT 'RU para UMSA; username para externos',
    nombres           VARCHAR(100)    NOT NULL,
    apellidos         VARCHAR(100)    NOT NULL,
    email             VARCHAR(120)    NOT NULL UNIQUE,
    email_verificado  BOOLEAN         NOT NULL DEFAULT FALSE,
    password_hash     VARCHAR(255)    NULL             COMMENT 'Solo externos; NULL para usuarios UMSA',
    estado            ENUM('ACTIVO','INACTIVO') NOT NULL DEFAULT 'ACTIVO',
    fecha_registro    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_email       (email),
    INDEX idx_username    (username),
    INDEX idx_estado      (estado)
) ENGINE=InnoDB COMMENT='Entidad base de la jerarquía de usuarios';


CREATE TABLE rol (
    id_rol       BIGINT        AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(50)   NOT NULL UNIQUE COMMENT 'ADMINISTRADOR|COORDINADOR|DOCENTE|PARTICIPANTE|AUXILIAR|DISEÑADOR',
    descripcion  TEXT          NULL
) ENGINE=InnoDB COMMENT='Catálogo de roles del sistema';

INSERT INTO rol (nombre, descripcion) VALUES
('ADMINISTRADOR', 'Acceso completo al sistema'),
('COORDINADOR',   'Gestión de cursos y eventos de su carrera'),
('DOCENTE',       'Registro de calificaciones y confirmación para emisión de certificados'),
('PARTICIPANTE',  'Inscripción a cursos y eventos, descarga de certificados'),
('AUXILIAR',      'Registro de asistencia en eventos asignados'),
('DISEÑADOR',     'Gestión de plantillas de certificados');


CREATE TABLE usuario_rol (
    id_usuario       BIGINT    NOT NULL,
    id_rol           BIGINT    NOT NULL,
    fecha_asignacion DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    asignado_por     BIGINT    NULL     COMMENT 'Usuario (administrador) que asignó el rol',

    PRIMARY KEY (id_usuario, id_rol),
    FOREIGN KEY (id_usuario)   REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    FOREIGN KEY (id_rol)       REFERENCES rol(id_rol)         ON DELETE CASCADE,
    FOREIGN KEY (asignado_por) REFERENCES usuario(id_usuario) ON DELETE SET NULL,

    INDEX idx_rol (id_rol)
) ENGINE=InnoDB COMMENT='Tabla intermedia N:M — usuario × rol';


CREATE TABLE docente (
    id_usuario  BIGINT       NOT NULL,
    titulo      VARCHAR(50)  NOT NULL COMMENT 'Lic., MSc., PhD., etc.',

    PRIMARY KEY (id_usuario),
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Atributos específicos del rol DOCENTE';


CREATE TABLE participante (
    id_usuario        BIGINT  NOT NULL,
    tipo_participante ENUM('UMSA','EXTERNO') NOT NULL COMMENT 'Determina el precio aplicado en inscripción',

    PRIMARY KEY (id_usuario),
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE,

    INDEX idx_tipo (tipo_participante)
) ENGINE=InnoDB COMMENT='Atributos específicos del rol PARTICIPANTE';


CREATE TABLE codigo_verificacion (
    id_codigo        BIGINT      AUTO_INCREMENT PRIMARY KEY,
    id_usuario       BIGINT      NOT NULL,
    codigo           VARCHAR(6)  NOT NULL,
    tipo             ENUM('EMAIL','REGISTRO') NOT NULL,
    usado            BOOLEAN     NOT NULL DEFAULT FALSE,
    fecha_creacion   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion DATETIME    NOT NULL COMMENT 'Válido por 24 horas desde la creación',
    fecha_uso        DATETIME    NULL     COMMENT 'Momento en que fue ingresado por el usuario',

    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE,

    INDEX idx_usuario  (id_usuario),
    INDEX idx_codigo   (codigo),
    INDEX idx_usado    (usado)
) ENGINE=InnoDB COMMENT='Códigos de 6 dígitos para verificar email de usuarios externos';


CREATE TABLE refresh_token (
    id_refresh       BIGINT       AUTO_INCREMENT PRIMARY KEY,
    id_usuario       BIGINT       NOT NULL,
    token            VARCHAR(255) NOT NULL UNIQUE,
    revocado         BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_creacion   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion DATETIME     NOT NULL,

    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE,

    INDEX idx_usuario (id_usuario),
    INDEX idx_revocado (revocado)
) ENGINE=InnoDB COMMENT='Refresh tokens para renovar JWT';


-- ============================================================
-- MÓDULO 2: ESTRUCTURA ACADÉMICA
-- ============================================================

CREATE TABLE carrera (
    id_carrera     BIGINT        AUTO_INCREMENT PRIMARY KEY,
    nombre         VARCHAR(150)  NOT NULL,
    estado         ENUM('ACTIVA','INACTIVA') NOT NULL DEFAULT 'ACTIVA',

    INDEX idx_estado (estado)
) ENGINE=InnoDB COMMENT='Carreras académicas de la FHCE';


CREATE TABLE coordinador_carrera (
    id_coordinador   BIGINT    NOT NULL COMMENT 'FK al usuario con rol COORDINADOR',
    id_carrera       BIGINT    NOT NULL,
    fecha_asignacion DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id_coordinador, id_carrera),
    FOREIGN KEY (id_coordinador) REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    FOREIGN KEY (id_carrera)     REFERENCES carrera(id_carrera) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Tabla intermedia N:M — coordinador × carrera';


-- ============================================================
-- MÓDULO 3: CURSOS Y EVENTOS
-- ============================================================

CREATE TABLE curso (
    id_curso         BIGINT          AUTO_INCREMENT PRIMARY KEY,
    id_carrera       BIGINT          NOT NULL,
    id_organizador   BIGINT          NOT NULL  COMMENT 'FK al usuario con rol COORDINADOR',
    id_disenador     BIGINT          NULL      COMMENT 'FK al usuario con rol DISEÑADOR',
    nombre           VARCHAR(200)    NOT NULL,
    descripcion      TEXT            NULL,
    lugar            VARCHAR(255)    NULL,
    imagen           VARCHAR(255)    NULL,
    carga_horaria    INT             NOT NULL  COMMENT 'Total de horas académicas',
    duracion         INT             NULL      COMMENT 'Duración numérica del curso',
    unidad           ENUM('días','semanas','meses') NULL COMMENT 'Unidad de duración del curso',
    fecha_inicio     DATE            NOT NULL,
    costo_externo    DECIMAL(10,2)   NOT NULL DEFAULT 0.00 COMMENT 'Precio para participantes externos',
    costo_umsa       DECIMAL(10,2)   NOT NULL DEFAULT 0.00 COMMENT 'Precio preferencial UMSA',
    nota_aprobacion  DECIMAL(5,2)    NOT NULL  COMMENT 'Nota mínima para aprobar',
    estado           ENUM('ABIERTO','LLENO','FINALIZADO') NOT NULL DEFAULT 'ABIERTO',
    fecha_creacion   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (id_carrera)     REFERENCES carrera(id_carrera)   ON DELETE RESTRICT,
    FOREIGN KEY (id_organizador) REFERENCES usuario(id_usuario)   ON DELETE RESTRICT,
    FOREIGN KEY (id_disenador)   REFERENCES usuario(id_usuario)   ON DELETE SET NULL,

    INDEX idx_carrera     (id_carrera),
    INDEX idx_organizador (id_organizador),
    INDEX idx_disenador   (id_disenador),
    INDEX idx_estado      (estado),
    INDEX idx_fecha       (fecha_inicio)
) ENGINE=InnoDB COMMENT='Cursos complementarios. Certificación por aprobación de nota.';


CREATE TABLE paralelo (
    id_curso             BIGINT        NOT NULL  COMMENT 'Parte de PK compuesta — FK a curso',
    codigo               VARCHAR(10)   NOT NULL  COMMENT 'Clave parcial: A, B, 01, etc.',
    id_docente           BIGINT        NULL      COMMENT 'FK al usuario con rol DOCENTE',
    modalidad            ENUM('PRESENCIAL','VIRTUAL','MIXTO') NOT NULL,
    cupo_maximo          INT           NULL,
    horario_descripcion  VARCHAR(255)  NULL,
    link                 VARCHAR(255)  NULL      COMMENT 'Enlace a clase virtual (si aplica)',

    PRIMARY KEY (id_curso, codigo),
    FOREIGN KEY (id_curso)   REFERENCES curso(id_curso)     ON DELETE CASCADE,
    FOREIGN KEY (id_docente) REFERENCES usuario(id_usuario) ON DELETE SET NULL,

    INDEX idx_docente (id_docente)
) ENGINE=InnoDB COMMENT='Entidad débil de CURSO — clave compuesta (id_curso, codigo)';


CREATE TABLE evento (
    id_evento       BIGINT          AUTO_INCREMENT PRIMARY KEY,
    id_carrera      BIGINT          NOT NULL,
    id_organizador  BIGINT          NOT NULL  COMMENT 'FK al usuario con rol COORDINADOR',
    id_disenador    BIGINT          NULL      COMMENT 'FK al usuario con rol DISEÑADOR',
    nombre          VARCHAR(200)    NOT NULL,
    descripcion     TEXT            NULL,
    lugar           VARCHAR(255)    NULL,
    imagen          VARCHAR(255)    NULL,
    carga_horaria   INT             NOT NULL  COMMENT 'Total de horas académicas',
    modalidad       ENUM('PRESENCIAL','VIRTUAL','MIXTO') NOT NULL,
    fecha_hora      DATETIME        NOT NULL  COMMENT 'Fecha y hora del evento (un solo día)',
    cupo_maximo     INT             NULL,
    costo_externo   DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    costo_umsa      DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    estado          ENUM('ABIERTO','LLENO','FINALIZADO') NOT NULL DEFAULT 'ABIERTO',
    fecha_creacion  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    link            VARCHAR(255)    NULL      COMMENT 'Enlace a evento virtual (si aplica)',

    FOREIGN KEY (id_carrera)     REFERENCES carrera(id_carrera) ON DELETE RESTRICT,
    FOREIGN KEY (id_organizador) REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
    FOREIGN KEY (id_disenador)   REFERENCES usuario(id_usuario) ON DELETE SET NULL,

    INDEX idx_carrera     (id_carrera),
    INDEX idx_organizador (id_organizador),
    INDEX idx_disenador   (id_disenador),
    INDEX idx_estado      (estado),
    INDEX idx_fecha_hora  (fecha_hora)
) ENGINE=InnoDB COMMENT='Eventos facultativos. Certificación por asistencia.';


CREATE TABLE auxiliar_evento (
    id_auxiliar      BIGINT    NOT NULL COMMENT 'FK al usuario con rol AUXILIAR',
    id_evento        BIGINT    NOT NULL,
    fecha_asignacion DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id_auxiliar, id_evento),
    FOREIGN KEY (id_auxiliar) REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    FOREIGN KEY (id_evento)   REFERENCES evento(id_evento)   ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Tabla intermedia N:M — auxiliar × evento';


-- ============================================================
-- MÓDULO 4: INSCRIPCIONES Y PAGOS
-- ============================================================

CREATE TABLE inscripcion (
    id_inscripcion    BIGINT          AUTO_INCREMENT PRIMARY KEY,
    id_participante   BIGINT          NOT NULL   COMMENT 'FK al usuario con rol PARTICIPANTE',
    id_curso          BIGINT          NULL       COMMENT 'NULL si es inscripción a evento',
    id_evento         BIGINT          NULL       COMMENT 'NULL si es inscripción a curso',
    codigo_paralelo   VARCHAR(10)     NULL       COMMENT 'Con id_curso forma FK compuesta hacia paralelo',
    tipo_precio       ENUM('UMSA','EXTERNO') NOT NULL,
    saldo             DECIMAL(10,2)   NOT NULL   COMMENT 'Monto que debe pagar el participante',
    fecha_inscripcion DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado            ENUM('PENDIENTE','CONFIRMADA','CANCELADA') NOT NULL DEFAULT 'PENDIENTE',

    FOREIGN KEY (id_participante) REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    FOREIGN KEY (id_curso)        REFERENCES curso(id_curso)     ON DELETE CASCADE,
    FOREIGN KEY (id_evento)       REFERENCES evento(id_evento)   ON DELETE CASCADE,

    -- FK compuesta hacia paralelo (solo cuando id_curso y codigo_paralelo no son NULL)
    FOREIGN KEY (id_curso, codigo_paralelo) REFERENCES paralelo(id_curso, codigo) ON DELETE SET NULL,

    -- Un participante no puede inscribirse dos veces a la misma actividad
    UNIQUE KEY uq_participante_curso  (id_participante, id_curso),
    UNIQUE KEY uq_participante_evento (id_participante, id_evento),

    INDEX idx_participante (id_participante),
    INDEX idx_curso        (id_curso),
    INDEX idx_evento       (id_evento),
    INDEX idx_estado       (estado)

) ENGINE=InnoDB COMMENT='Inscripción de un participante a un curso o evento';


CREATE TABLE pago (
    id_pago                 BIGINT          AUTO_INCREMENT PRIMARY KEY,
    id_inscripcion          BIGINT          NOT NULL,
    monto                   DECIMAL(10,2)   NOT NULL   COMMENT 'Monto efectivamente pagado',
    metodo_pago             VARCHAR(50)     NULL       COMMENT 'Método usado en pasarela Libélula',
    referencia_transaccion  VARCHAR(150)    NULL       COMMENT 'ID de transacción devuelto por Libélula',
    estado                  ENUM('PENDIENTE','APROBADO','RECHAZADO') NOT NULL DEFAULT 'PENDIENTE',
    fecha_pago              DATETIME        NULL       COMMENT 'Momento de confirmación del pago',
    fecha_registro          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (id_inscripcion) REFERENCES inscripcion(id_inscripcion) ON DELETE CASCADE,

    INDEX idx_inscripcion  (id_inscripcion),
    INDEX idx_estado       (estado),
    INDEX idx_referencia   (referencia_transaccion)
) ENGINE=InnoDB COMMENT='Transacción de pago asociada a una inscripción';


-- ============================================================
-- MÓDULO 5: EVALUACIÓN Y ASISTENCIA
-- ============================================================

CREATE TABLE evaluacion_estudiante (
    id_evaluacion  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    id_inscripcion BIGINT        NOT NULL UNIQUE COMMENT 'Relación 1:1 con inscripcion',
    nota_final     DECIMAL(5,2)  NOT NULL COMMENT 'Nota sobre 100',
    estado         ENUM('APROBADO','REPROBADO') NOT NULL,
    fecha_registro DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (id_inscripcion) REFERENCES inscripcion(id_inscripcion) ON DELETE CASCADE,

    INDEX idx_estado (estado)
) ENGINE=InnoDB COMMENT='Nota final de un participante en un curso. Relación 1:1 con inscripcion.';


CREATE TABLE historial (
    id_historial   BIGINT        AUTO_INCREMENT PRIMARY KEY,
    id_evaluacion  BIGINT        NOT NULL,
    nota_anterior  DECIMAL(5,2)  NOT NULL,
    nota_nueva     DECIMAL(5,2)  NOT NULL,
    motivo         TEXT          NOT NULL   COMMENT 'Justificación del cambio de nota',
    cambiado_por   BIGINT        NOT NULL   COMMENT 'Usuario que realizó el cambio (administrador)',
    fecha_cambio   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (id_evaluacion) REFERENCES evaluacion_estudiante(id_evaluacion) ON DELETE CASCADE,
    FOREIGN KEY (cambiado_por)  REFERENCES usuario(id_usuario)                  ON DELETE RESTRICT,

    INDEX idx_evaluacion (id_evaluacion)
) ENGINE=InnoDB COMMENT='Auditoría de cambios de nota. Cada edición genera un nuevo registro.';


CREATE TABLE asistencia (
    id_asistencia  BIGINT    AUTO_INCREMENT PRIMARY KEY,
    id_inscripcion BIGINT    NOT NULL UNIQUE COMMENT 'Si existe el registro, el participante asistió',
    registrado_por BIGINT    NOT NULL        COMMENT 'Coordinador o auxiliar que registró la asistencia',
    fecha_registro DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (id_inscripcion) REFERENCES inscripcion(id_inscripcion) ON DELETE CASCADE,
    FOREIGN KEY (registrado_por) REFERENCES usuario(id_usuario)         ON DELETE RESTRICT,

    INDEX idx_registrado_por (registrado_por)
) ENGINE=InnoDB COMMENT='Registro de asistencia a evento. Existencia del registro = asistió.';


CREATE TABLE solicitud_emision (
    id_solicitud        BIGINT        AUTO_INCREMENT PRIMARY KEY,
    id_curso            BIGINT        NULL  COMMENT 'NULL si es evento',
    codigo_paralelo     VARCHAR(10)   NULL  COMMENT 'Con id_curso forma FK compuesta hacia paralelo',
    id_evento           BIGINT        NULL  COMMENT 'NULL si es curso',
    id_docente          BIGINT        NOT NULL COMMENT 'Docente que confirmó las calificaciones',
    cantidad_aprobados  INT           NOT NULL,
    estado              ENUM('PENDIENTE','EN_PROCESO','COMPLETADO') NOT NULL DEFAULT 'PENDIENTE',
    procesado_por       BIGINT        NULL  COMMENT 'Coordinador que procesó la solicitud',
    fecha_solicitud     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_procesamiento DATETIME      NULL,
    notas               TEXT          NULL  COMMENT 'Observaciones adicionales del docente',

    FOREIGN KEY (id_curso)     REFERENCES curso(id_curso)     ON DELETE CASCADE,
    FOREIGN KEY (id_evento)    REFERENCES evento(id_evento)   ON DELETE CASCADE,
    FOREIGN KEY (id_docente)   REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
    FOREIGN KEY (procesado_por)REFERENCES usuario(id_usuario) ON DELETE SET NULL,
    FOREIGN KEY (id_curso, codigo_paralelo) REFERENCES paralelo(id_curso, codigo) ON DELETE SET NULL,

    INDEX idx_estado          (estado),
    INDEX idx_docente         (id_docente),
    INDEX idx_fecha_solicitud (fecha_solicitud)

) ENGINE=InnoDB COMMENT='Generada cuando el docente confirma notas. Flujo: PENDIENTE→EN_PROCESO→COMPLETADO';


-- ============================================================
-- MÓDULO 6: PLANTILLAS Y CERTIFICADOS
-- ============================================================

CREATE TABLE plantilla_certificado (
    id_plantilla  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    id_curso      BIGINT        NULL  COMMENT 'NULL si es plantilla de evento',
    id_evento     BIGINT        NULL  COMMENT 'NULL si es plantilla de curso',
    archivo_pdf   VARCHAR(255)  NOT NULL COMMENT 'Ruta del archivo PDF subido',
    version       INT           NOT NULL DEFAULT 1,
    subida_por    BIGINT        NOT NULL COMMENT 'FK al usuario con rol DISEÑADOR',
    fecha_subida  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado        ENUM('PENDIENTE','VIGENTE','HISTORICA') NOT NULL DEFAULT 'PENDIENTE',

    FOREIGN KEY (id_curso)   REFERENCES curso(id_curso)     ON DELETE CASCADE,
    FOREIGN KEY (id_evento)  REFERENCES evento(id_evento)   ON DELETE CASCADE,
    FOREIGN KEY (subida_por) REFERENCES usuario(id_usuario) ON DELETE RESTRICT,

    INDEX idx_curso   (id_curso),
    INDEX idx_evento  (id_evento),
    INDEX idx_estado  (estado)

) ENGINE=InnoDB COMMENT='Plantilla PDF para certificados. Solo una VIGENTE por actividad.';


CREATE TABLE aprobacion (
    id_aprobacion   BIGINT    AUTO_INCREMENT PRIMARY KEY,
    id_plantilla    BIGINT    NOT NULL,
    id_coordinador  BIGINT    NOT NULL COMMENT 'FK al usuario con rol COORDINADOR',
    estado          ENUM('PENDIENTE','APROBADA','RECHAZADA') NOT NULL DEFAULT 'PENDIENTE',
    observaciones   TEXT      NULL,
    fecha_revision  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (id_plantilla)   REFERENCES plantilla_certificado(id_plantilla) ON DELETE CASCADE,
    FOREIGN KEY (id_coordinador) REFERENCES usuario(id_usuario)                 ON DELETE RESTRICT,

    INDEX idx_plantilla    (id_plantilla),
    INDEX idx_coordinador  (id_coordinador),
    INDEX idx_estado       (estado)
) ENGINE=InnoDB COMMENT='Revisión de plantilla por coordinador. Cada revisión genera un nuevo registro.';


CREATE TABLE certificado (
    id_certificado      BIGINT        AUTO_INCREMENT PRIMARY KEY,
    id_inscripcion      BIGINT        NOT NULL,
    codigo_verificacion VARCHAR(100)  NOT NULL UNIQUE COMMENT 'Código único para verificación QR',
    archivo_generado    VARCHAR(255)  NULL,
    estado_emision      ENUM('GENERADO','ANULADO','REEMITIDO') NOT NULL DEFAULT 'GENERADO',
    version             INT           NOT NULL DEFAULT 1 COMMENT 'Incrementa en cada reemisión',
    fecha_emision       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (id_inscripcion) REFERENCES inscripcion(id_inscripcion) ON DELETE CASCADE,

    INDEX idx_inscripcion    (id_inscripcion),
    INDEX idx_estado_emision (estado_emision),
    INDEX idx_version        (version)
) ENGINE=InnoDB COMMENT='Certificado digital emitido. Control de versiones para anulaciones y reemisiones.';


CREATE TABLE c_anulacion (
    id_anulacion              BIGINT    AUTO_INCREMENT PRIMARY KEY,
    id_certificado            BIGINT    NOT NULL UNIQUE COMMENT 'Relación 1:1 con certificado',
    id_usuario                BIGINT    NOT NULL COMMENT 'Usuario que realizó la anulación',
    motivo_anulacion          TEXT      NOT NULL,
    id_certificado_reemplazo  BIGINT    NULL     COMMENT 'Nuevo certificado emitido (si aplica). Relación 1:1.',
    fecha_anulacion           DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (id_certificado)           REFERENCES certificado(id_certificado) ON DELETE CASCADE,
    FOREIGN KEY (id_usuario)               REFERENCES usuario(id_usuario)         ON DELETE RESTRICT,
    FOREIGN KEY (id_certificado_reemplazo) REFERENCES certificado(id_certificado) ON DELETE SET NULL,

    INDEX idx_usuario (id_usuario)
) ENGINE=InnoDB COMMENT='Anulación de certificado. id_certificado_reemplazo NULL si no hubo reemisión.';


-- ============================================================
-- TRIGGERS
-- ============================================================

DELIMITER //

-- ── T1: Calcular estado (APROBADO/REPROBADO) al insertar nota ────────────────
CREATE TRIGGER trg_estado_evaluacion
BEFORE INSERT ON evaluacion_estudiante
FOR EACH ROW
BEGIN
    DECLARE nota_minima DECIMAL(5,2);

    SELECT c.nota_aprobacion INTO nota_minima
    FROM curso c
    JOIN inscripcion i ON i.id_curso = c.id_curso
    WHERE i.id_inscripcion = NEW.id_inscripcion;

    IF NEW.nota_final >= nota_minima THEN
        SET NEW.estado = 'APROBADO';
    ELSE
        SET NEW.estado = 'REPROBADO';
    END IF;
END//


-- ── T2: Recalcular estado al actualizar nota ─────────────────────────────────
CREATE TRIGGER trg_update_estado_evaluacion
BEFORE UPDATE ON evaluacion_estudiante
FOR EACH ROW
BEGIN
    DECLARE nota_minima DECIMAL(5,2);

    SELECT c.nota_aprobacion INTO nota_minima
    FROM curso c
    JOIN inscripcion i ON i.id_curso = c.id_curso
    WHERE i.id_inscripcion = NEW.id_inscripcion;

    IF NEW.nota_final >= nota_minima THEN
        SET NEW.estado = 'APROBADO';
    ELSE
        SET NEW.estado = 'REPROBADO';
    END IF;
END//


-- ── T3: Confirmar inscripción automáticamente si la actividad es gratuita ────
CREATE TRIGGER trg_confirmar_inscripcion_gratuita
AFTER INSERT ON inscripcion
FOR EACH ROW
BEGIN
    DECLARE es_gratis BOOLEAN DEFAULT FALSE;

    IF NEW.id_curso IS NOT NULL THEN
        SELECT (costo_externo = 0 AND costo_umsa = 0)
        INTO es_gratis
        FROM curso WHERE id_curso = NEW.id_curso;
    ELSE
        SELECT (costo_externo = 0 AND costo_umsa = 0)
        INTO es_gratis
        FROM evento WHERE id_evento = NEW.id_evento;
    END IF;

    IF es_gratis = TRUE THEN
        UPDATE inscripcion
        SET estado = 'CONFIRMADA'
        WHERE id_inscripcion = NEW.id_inscripcion;
    END IF;
END//


-- ── T4: Confirmar inscripción cuando el pago es APROBADO ────────────────────
CREATE TRIGGER trg_confirmar_inscripcion_por_pago
AFTER UPDATE ON pago
FOR EACH ROW
BEGIN
    IF NEW.estado = 'APROBADO' AND OLD.estado != 'APROBADO' THEN
        UPDATE inscripcion
        SET estado = 'CONFIRMADA'
        WHERE id_inscripcion = NEW.id_inscripcion
          AND estado = 'PENDIENTE';
    END IF;
END//


-- ── T5: Actualizar estado del curso cuando un paralelo se llena ──────────────
--        El curso solo se marca LLENO cuando TODOS sus paralelos están llenos.
CREATE TRIGGER trg_cupo_paralelo
AFTER UPDATE ON inscripcion
FOR EACH ROW
BEGIN
    DECLARE total_inscritos   INT;
    DECLARE cupo_par          INT;
    DECLARE paralelos_con_cupo INT;
    DECLARE paralelos_llenos  INT;

    IF NEW.estado = 'CONFIRMADA' AND OLD.estado != 'CONFIRMADA'
       AND NEW.id_curso IS NOT NULL AND NEW.codigo_paralelo IS NOT NULL THEN

        -- Cupo del paralelo específico
        SELECT cupo_maximo INTO cupo_par
        FROM paralelo
        WHERE id_curso = NEW.id_curso AND codigo = NEW.codigo_paralelo;

        IF cupo_par IS NOT NULL THEN

            -- Inscritos confirmados en ese paralelo
            SELECT COUNT(*) INTO total_inscritos
            FROM inscripcion
            WHERE id_curso = NEW.id_curso
              AND codigo_paralelo = NEW.codigo_paralelo
              AND estado = 'CONFIRMADA';

            IF total_inscritos >= cupo_par THEN

                -- Paralelos con cupo definido en el curso
                SELECT COUNT(*) INTO paralelos_con_cupo
                FROM paralelo
                WHERE id_curso = NEW.id_curso AND cupo_maximo IS NOT NULL;

                -- Paralelos que ya alcanzaron su cupo
                SELECT COUNT(*) INTO paralelos_llenos
                FROM paralelo p
                WHERE p.id_curso = NEW.id_curso
                  AND p.cupo_maximo IS NOT NULL
                  AND (
                      SELECT COUNT(*) FROM inscripcion i
                      WHERE i.id_curso = p.id_curso
                        AND i.codigo_paralelo = p.codigo
                        AND i.estado = 'CONFIRMADA'
                  ) >= p.cupo_maximo;

                -- Solo marcar LLENO si todos los paralelos están llenos
                IF paralelos_llenos >= paralelos_con_cupo THEN
                    UPDATE curso SET estado = 'LLENO'
                    WHERE id_curso = NEW.id_curso;
                END IF;

            END IF;
        END IF;
    END IF;
END//


-- ── T6: Actualizar estado del evento a LLENO cuando se alcanza el cupo ───────
CREATE TRIGGER trg_cupo_evento
AFTER UPDATE ON inscripcion
FOR EACH ROW
BEGIN
    DECLARE total_inscritos INT;
    DECLARE cupo_ev         INT;

    IF NEW.estado = 'CONFIRMADA' AND OLD.estado != 'CONFIRMADA'
       AND NEW.id_evento IS NOT NULL THEN

        SELECT cupo_maximo INTO cupo_ev
        FROM evento WHERE id_evento = NEW.id_evento;

        IF cupo_ev IS NOT NULL THEN
            SELECT COUNT(*) INTO total_inscritos
            FROM inscripcion
            WHERE id_evento = NEW.id_evento AND estado = 'CONFIRMADA';

            IF total_inscritos >= cupo_ev THEN
                UPDATE evento SET estado = 'LLENO'
                WHERE id_evento = NEW.id_evento;
            END IF;
        END IF;
    END IF;
END//


-- ── T7: Validar requisitos antes de emitir un certificado ───────────────────
--        Curso  → debe tener evaluacion con estado APROBADO
--        Evento → debe tener registro de asistencia
CREATE TRIGGER trg_validar_certificado
BEFORE INSERT ON certificado
FOR EACH ROW
BEGIN
    DECLARE es_curso INT DEFAULT 0;
    DECLARE cumple   INT DEFAULT 0;

    SELECT COUNT(*) INTO es_curso
    FROM inscripcion
    WHERE id_inscripcion = NEW.id_inscripcion AND id_curso IS NOT NULL;

    IF es_curso > 0 THEN
        SELECT COUNT(*) INTO cumple
        FROM evaluacion_estudiante
        WHERE id_inscripcion = NEW.id_inscripcion AND estado = 'APROBADO';
    ELSE
        SELECT COUNT(*) INTO cumple
        FROM asistencia
        WHERE id_inscripcion = NEW.id_inscripcion;
    END IF;

    IF cumple = 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'El participante no cumple los requisitos para recibir un certificado';
    END IF;
END//


-- ── T8: Validar que el tipo de precio coincida con el tipo de participante ───
--        Se omite la validación si la actividad es gratuita (saldo = 0).
CREATE TRIGGER trg_validar_tipo_precio
BEFORE INSERT ON inscripcion
FOR EACH ROW
BEGIN
    DECLARE tipo_part ENUM('UMSA','EXTERNO');
    DECLARE es_gratis INT DEFAULT 0;

    -- Verificar si la actividad es gratuita
    IF NEW.id_curso IS NOT NULL THEN
        SELECT COUNT(*) INTO es_gratis
        FROM curso
        WHERE id_curso = NEW.id_curso AND costo_externo = 0 AND costo_umsa = 0;
    ELSE
        SELECT COUNT(*) INTO es_gratis
        FROM evento
        WHERE id_evento = NEW.id_evento AND costo_externo = 0 AND costo_umsa = 0;
    END IF;

    -- Solo validar si la actividad tiene costo
    IF es_gratis = 0 THEN
        SELECT tipo_participante INTO tipo_part
        FROM participante
        WHERE id_usuario = NEW.id_participante;

        IF tipo_part != NEW.tipo_precio THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El tipo de precio no coincide con el tipo de participante';
        END IF;
    END IF;
END//


-- ── T9: Marcar certificado como ANULADO al registrar una anulación ───────────
CREATE TRIGGER trg_anular_certificado
AFTER INSERT ON c_anulacion
FOR EACH ROW
BEGIN
    UPDATE certificado
    SET estado_emision = IF(NEW.id_certificado_reemplazo IS NULL, 'ANULADO', 'REEMITIDO')
    WHERE id_certificado = NEW.id_certificado;
END//


-- ── T10: Marcar certificado como REEMITIDO cuando se asigna un reemplazo ─────
CREATE TRIGGER trg_marcar_reemitido
AFTER UPDATE ON c_anulacion
FOR EACH ROW
BEGIN
    IF NEW.id_certificado_reemplazo IS NOT NULL
       AND OLD.id_certificado_reemplazo IS NULL THEN
        UPDATE certificado
        SET estado_emision = 'REEMITIDO'
        WHERE id_certificado = NEW.id_certificado;
    END IF;
END//

DELIMITER ;
