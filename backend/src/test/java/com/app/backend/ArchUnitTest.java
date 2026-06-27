package com.app.backend;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.app.backend", importOptions = com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests.class)
class ArchUnitTest {

    @ArchTest
    static final ArchRule controllers_must_not_access_repositories =
            noClasses()
                    .that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat().resideInAPackage("..repository..")
                    .as("Controllers must go through services, not repositories directly");

    @ArchTest
    static final ArchRule services_must_not_import_controllers =
            noClasses()
                    .that().resideInAPackage("..service..")
                    .should().dependOnClassesThat().resideInAPackage("..controller..")
                    .as("Services must not depend on controller classes");

    @ArchTest
    static final ArchRule entities_must_not_depend_on_services_or_controllers =
            noClasses()
                    .that().resideInAPackage("..entity..")
                    .should().dependOnClassesThat().resideInAnyPackage("..service..", "..controller..")
                    .as("Entities must not depend on service or controller classes");

    @ArchTest
    static final ArchRule controllers_must_be_annotated =
            classes()
                    .that().resideInAPackage("..controller..")
                    .and().doNotHaveSimpleName("package-info")
                    .should().beAnnotatedWith(RestController.class)
                    .orShould().beAnnotatedWith(RestControllerAdvice.class)
                    .as("Controllers must be annotated with @RestController or @RestControllerAdvice");

    @ArchTest
    static final ArchRule services_must_be_annotated =
            classes()
                    .that().resideInAPackage("..service..")
                    .and().areNotAnonymousClasses()
                    .and().areNotLocalClasses()
                    .and().doNotHaveSimpleName("package-info")
                    .should().beAnnotatedWith(Service.class)
                    .as("Services must be annotated with @Service");
}
