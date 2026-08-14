-- =====================================================================
-- Finance AI — DDL para OCI Autonomous Database (Oracle)
-- Ejecutar en el orden dado: respeta las dependencias de llaves foráneas
-- =====================================================================

-- 1. Usuario
CREATE TABLE usuario (
    id_usuario           VARCHAR2(36)   NOT NULL,
    ingreso_mensual      NUMBER(12,2)   NOT NULL,
    nivel_endeudamiento  NUMBER(5,2)    NOT NULL,
    frecuencia_ahorro    VARCHAR2(20)   NOT NULL,
    fecha_registro       TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT ck_usuario_ahorro CHECK (frecuencia_ahorro IN ('Baja','Media','Alta'))
);

-- 2. Catálogo de categorías (compartido entre transacciones bancarias y registros manuales)
CREATE TABLE categoria_gasto (
    id_categoria     NUMBER         NOT NULL,
    nombre_categoria VARCHAR2(100)  NOT NULL,
    CONSTRAINT pk_categoria_gasto PRIMARY KEY (id_categoria),
    CONSTRAINT uq_categoria_nombre UNIQUE (nombre_categoria)
);

-- 3. Cartola cargada (evento de carga; un usuario puede subir varias, de distintos bancos)
CREATE TABLE cartola_cargada (
    id_cartola      VARCHAR2(36)   NOT NULL,
    id_usuario      VARCHAR2(36)   NOT NULL,
    banco_origen    VARCHAR2(50)   NOT NULL,
    tipo_cartola    VARCHAR2(20)   NOT NULL,
    periodo_inicio  DATE           NOT NULL,
    periodo_fin     DATE           NOT NULL,
    fecha_carga     TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    nombre_archivo  VARCHAR2(255),
    CONSTRAINT pk_cartola_cargada PRIMARY KEY (id_cartola),
    CONSTRAINT fk_cartola_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT ck_cartola_tipo CHECK (tipo_cartola IN ('CUENTA_CORRIENTE','BILLETERA_DIGITAL'))
);

-- 4. Transacción (proviene de una cartola; categoría asignada automáticamente por el modelo)
CREATE TABLE transaccion (
    nro_operacion        VARCHAR2(36)   NOT NULL,
    id_cartola           VARCHAR2(36)   NOT NULL,
    id_categoria         NUMBER,
    fecha_transaccion    DATE           NOT NULL,
    descripcion          VARCHAR2(255)  NOT NULL,
    monto_abono          NUMBER(12,2),
    monto_cargo          NUMBER(12,2),
    saldo_restante       NUMBER(12,2),
    metodo_clasificacion VARCHAR2(20),
    probabilidad_modelo  NUMBER(4,3),
    CONSTRAINT pk_transaccion PRIMARY KEY (nro_operacion),
    CONSTRAINT fk_transaccion_cartola FOREIGN KEY (id_cartola) REFERENCES cartola_cargada (id_cartola),
    CONSTRAINT fk_transaccion_categoria FOREIGN KEY (id_categoria) REFERENCES categoria_gasto (id_categoria),
    CONSTRAINT ck_transaccion_metodo CHECK (metodo_clasificacion IN ('MAPEO','REGLA','MODELO','FALLBACK','CORREGIDO_USUARIO'))
);

-- 5. Registro manual (gasto autoreportado por el usuario, fuera de las transacciones bancarias;
--    puede vincularse opcionalmente a una transacción si coincide monto + fecha/hora + lugar)
CREATE TABLE registro_manual (
    id_registro              VARCHAR2(36)   NOT NULL,
    id_usuario               VARCHAR2(36)   NOT NULL,
    id_categoria             NUMBER,
    medio_pago               VARCHAR2(20)   NOT NULL,
    nro_operacion_vinculado  VARCHAR2(36),
    metodo_vinculacion       VARCHAR2(20)   DEFAULT 'SIN_VINCULAR' NOT NULL,
    fecha_gasto              DATE           NOT NULL,
    hora_gasto                VARCHAR2(8),
    lugar                     VARCHAR2(255),
    monto                     NUMBER(12,2)  NOT NULL,
    descripcion_usuario       VARCHAR2(255),
    fecha_registro             TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_registro_manual PRIMARY KEY (id_registro),
    CONSTRAINT fk_registro_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_registro_categoria FOREIGN KEY (id_categoria) REFERENCES categoria_gasto (id_categoria),
    CONSTRAINT fk_registro_transaccion FOREIGN KEY (nro_operacion_vinculado) REFERENCES transaccion (nro_operacion),
    CONSTRAINT ck_registro_medio_pago CHECK (medio_pago IN ('EFECTIVO','DEBITO')),
    CONSTRAINT ck_registro_metodo_vinc CHECK (metodo_vinculacion IN ('AUTOMATICO','CONFIRMADO_USUARIO','SIN_VINCULAR'))
);

-- 6. Análisis histórico (una fila por cada corrida de análisis, preserva evolución en el tiempo)
CREATE TABLE analisis_historial (
    id_analisis        VARCHAR2(36)   NOT NULL,
    id_usuario         VARCHAR2(36)   NOT NULL,
    fecha_analisis     TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    perfil_financiero  VARCHAR2(30)   NOT NULL,
    probabilidad       NUMBER(4,3)    NOT NULL,
    resumen_gastos     CLOB,
    CONSTRAINT pk_analisis_historial PRIMARY KEY (id_analisis),
    CONSTRAINT fk_analisis_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT ck_analisis_perfil CHECK (perfil_financiero IN ('Saludable','En observación','En riesgo')),
    CONSTRAINT ck_analisis_json CHECK (resumen_gastos IS JSON)
);

-- 7. Recomendación (ligada a un análisis puntual, no directamente al usuario)
CREATE TABLE recomendacion (
    id_recomendacion     VARCHAR2(36)   NOT NULL,
    id_analisis          VARCHAR2(36)   NOT NULL,
    texto_recomendacion  VARCHAR2(500)  NOT NULL,
    fecha_generacion     TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_recomendacion PRIMARY KEY (id_recomendacion),
    CONSTRAINT fk_recomendacion_analisis FOREIGN KEY (id_analisis) REFERENCES analisis_historial (id_analisis)
);

-- 8. Mapeo de categorías aprendido (nivel 1 del plan de clasificación híbrido).
--    Aporte del equipo de Backend, adaptado a sintaxis Oracle: la propuesta
--    original usaba BIGINT GENERATED BY DEFAULT AS IDENTITY, que no es válido
--    en Oracle/Autonomous DB.
CREATE TABLE categoria_mapeo (
    id_mapeo            NUMBER GENERATED BY DEFAULT AS IDENTITY,
    patron_descripcion  VARCHAR2(255)  NOT NULL,
    id_categoria        NUMBER         NOT NULL,
    frecuencia          NUMBER         DEFAULT 1 NOT NULL,
    CONSTRAINT pk_categoria_mapeo PRIMARY KEY (id_mapeo),
    CONSTRAINT uq_categoria_mapeo_patron UNIQUE (patron_descripcion),
    CONSTRAINT fk_categoria_mapeo_categoria FOREIGN KEY (id_categoria) REFERENCES categoria_gasto (id_categoria)
);

-- =====================================================================
-- Seed: catálogo inicial de categorías (las 7 del brief)
-- =====================================================================
INSERT INTO categoria_gasto (id_categoria, nombre_categoria) VALUES (1, 'Alimentación');
INSERT INTO categoria_gasto (id_categoria, nombre_categoria) VALUES (2, 'Transporte');
INSERT INTO categoria_gasto (id_categoria, nombre_categoria) VALUES (3, 'Salud');
INSERT INTO categoria_gasto (id_categoria, nombre_categoria) VALUES (4, 'Vivienda');
INSERT INTO categoria_gasto (id_categoria, nombre_categoria) VALUES (5, 'Educación');
INSERT INTO categoria_gasto (id_categoria, nombre_categoria) VALUES (6, 'Ocio');
INSERT INTO categoria_gasto (id_categoria, nombre_categoria) VALUES (7, 'Servicios');
COMMIT;
