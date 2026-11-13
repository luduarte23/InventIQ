package com.inventiq;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * Reglas de arquitectura de ACUERDOS §2, comprobadas en cada build.
 * ArchUnit lee las clases compiladas de src/main (no las de prueba) y revisa
 * de qué depende cada una.
 *
 * Nota: la capa de presentación incluye "web" porque ahí está hoy la pantalla de ventas (B).
 * Si el equipo la mueve a "presentation", se puede quitar "com.inventiq.web..".
 */
class ArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.inventiq");

    @Test
    void domainDoesNotDependOnFrameworks() {
        noClasses().that().resideInAPackage("com.inventiq.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "org.hibernate..",
                        "com.twilio..")
                .because("ACUERDOS §2: el dominio no importa nada de Spring, JPA ni Twilio")
                .check(CLASSES);
    }

    @Test
    void domainDoesNotDependOnOtherLayers() {
        noClasses().that().resideInAPackage("com.inventiq.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.inventiq.application..",
                        "com.inventiq.infrastructure..",
                        "com.inventiq.presentation..",
                        "com.inventiq.web..")
                .because("ACUERDOS §2: presentation -> application -> domain <- infrastructure")
                .check(CLASSES);
    }

    @Test
    void layersAreOnlyUsedInTheAllowedDirection() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .withOptionalLayers(true)
                .layer("Presentation").definedBy("com.inventiq.presentation..", "com.inventiq.web..")
                .layer("Application").definedBy("com.inventiq.application..")
                .layer("Domain").definedBy("com.inventiq.domain..")
                .layer("Infrastructure").definedBy("com.inventiq.infrastructure..")
                .whereLayer("Presentation").mayNotBeAccessedByAnyLayer()
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Presentation", "Infrastructure")
                .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
                .check(CLASSES);
    }

    @Test
    void domainNeverReadsTheSystemClock() {
        noClasses().that().resideInAPackage("com.inventiq.domain..")
                .should().callMethod(LocalDate.class, "now")
                .orShould().callMethod(LocalDateTime.class, "now")
                .orShould().callMethod(Instant.class, "now")
                .because("ACUERDOS §2: el dominio recibe la fecha o un Clock como parámetro")
                .check(CLASSES);
    }
}