package co.edu.unal.inventiq.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Convierte la regla "lógica de negocio aislada" en algo verificable:
 * las dependencias solo van web -> application -> domain <- infrastructure.
 */
@AnalyzeClasses(packages = "co.edu.unal.inventiq", importOptions = ImportOption.DoNotIncludeTests.class)
class LayerDependencyTest {

    @ArchTest
    static final ArchRule domainDoesNotDependOnFrameworks = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "org.hibernate..")
            .because("las reglas de negocio deben probarse sin Spring ni base de datos")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule layersRespectTheirDirection = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("Web").definedBy("..web..")
            .layer("Application").definedBy("..application..")
            .layer("Domain").definedBy("..domain..")
            .layer("Infrastructure").definedBy("..infrastructure..")
            .whereLayer("Web").mayNotBeAccessedByAnyLayer()
            // Infrastructure crea los servicios en infrastructure.config.
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Web", "Infrastructure")
            // Web puede leer el modelo de dominio para mostrarlo en las vistas.
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Web")
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer();
}
