package sn.syscad.traitement.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.Architectures;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ArchitectureRulesTest {

  private final JavaClasses classes =
      new ClassFileImporter().importPath(Path.of("target/classes"));

  @Test
  void domainShouldNotDependOnApiOrInfrastructure() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..api..", "..infrastructure..")
        .check(classes);
  }

  @Test
  void layeredArchitectureShouldBeRespected() {
    Architectures.layeredArchitecture()
        .consideringAllDependencies()
        .layer("Domain").definedBy("sn.syscad.traitement.domain..")
        .layer("Api").definedBy("sn.syscad.traitement.api..")
        .layer("Demo").definedBy("sn.syscad.traitement.demo..")
        .whereLayer("Api").mayNotBeAccessedByAnyLayer()
        .whereLayer("Demo").mayOnlyBeAccessedByLayers("Api")
        .check(classes);
  }
}
