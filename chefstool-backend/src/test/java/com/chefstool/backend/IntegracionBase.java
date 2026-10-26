package com.chefstool.backend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base de los tests que arrancan la aplicacion entera. Levantan su propio PostgreSQL con
 * Testcontainers, de modo que no hacen falta la base de datos local ni el archivo .env
 * (solo Docker). El contenedor se arranca una vez y lo comparten todas las clases.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class IntegracionBase {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
        // Sin depender del archivo .env local ni de variables de entorno
        registro.add(
                "chefstool.seguridad.jwt-secret",
                () -> "secreto-de-pruebas-con-mas-de-32-caracteres");
        registro.add(
                "chefstool.almacenamiento.directorio",
                () -> System.getProperty("java.io.tmpdir") + "/chefstool-test-uploads");
    }
}
