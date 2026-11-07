package com.forkmyfolio.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

import jakarta.persistence.Entity;

/**
 * Architectural enforcement (NAG-01 series).
 * <p>
 * These rules encode the <em>established</em> architecture of ForkMyFolio:
 * <ul>
 *   <li>DTOs exist only at the controller/API boundary; services operate on
 *       entities and never touch DTOs (DBR / Rule 2).</li>
 *   <li>Controllers translate DTOs via manual mappers and delegate to
 *       services; controllers never touch repositories and never call methods
 *       on or construct JPA entities.</li>
 *   <li>Directionality: controller &rarr; mapper/service &rarr; repository
 *       &rarr; model (nothing flows back up).</li>
 * </ul>
 * <p>
 * Precision notes (avoiding false positives):
 * <ul>
 *   <li>Passing an entity <em>received from a service</em> to another service
 *       or to a mapper is NOT a violation: only method calls whose target
 *       owner is an {@code @Entity} class, or entity construction, are
 *       flagged in controllers.</li>
 *   <li>DTOs may reference nested enums of entities (domain vocabulary such
 *       as {@code Experience.LocationType}); the rule only forbids DTO
 *       <em>fields</em> whose raw type is an {@code @Entity}.</li>
 *   <li>The self-invocation proxy of {@code AuthServiceImpl} (setter used for
 *       the registration retry transaction) is a legitimate, documented
 *       pattern; the injection rules target {@code @Autowired} FIELDS only.</li>
 * </ul>
 * Known legacy violations are frozen with {@link FreezingArchRule} (violation
 * store in {@code archunit_store/}); any NEW violation fails the build
 * immediately, and frozen ones are documented for separate remediation in
 * {@code docs/PHASE7_TRACKING.adoc}.
 */
public class ArchitectureTest {

    private static final Class<? extends java.lang.annotation.Annotation> AUTOWIRED =
            org.springframework.beans.factory.annotation.Autowired.class;
    private static final Class<? extends java.lang.annotation.Annotation> INJECT =
            jakarta.inject.Inject.class;

    /** Classes annotated with JPA @Entity. */
    private static final DescribedPredicate<JavaClass> ENTITY_CLASSES =
            new DescribedPredicate<>("classes annotated @Entity") {
                @Override
                public boolean test(JavaClass input) {
                    return input.isAnnotatedWith(Entity.class);
                }
            };

    /** Method calls whose target owner is a JPA entity. */
    private static final DescribedPredicate<JavaCall<?>> CALL_ON_ENTITY =
            new DescribedPredicate<>("calls on @Entity classes") {
                @Override
                public boolean test(JavaCall<?> call) {
                    return call.getTargetOwner().isAnnotatedWith(Entity.class);
                }
            };

    /** Test classes within com.forkmyfolio identified by naming convention. */
    private static final DescribedPredicate<JavaClass> TEST_CLASSES =
            new DescribedPredicate<>("test classes by naming convention") {
                @Override
                public boolean test(JavaClass input) {
                    // Anonymous/nested classes have bytecode names like
                    // com.forkmyfolio.<pkg>.OuterTest$4; classify by the
                    // top-level class part before any '$'.
                    String topLevel = input.getName().split("\\$")[0];
                    return topLevel.endsWith("Test");
                }
            };

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.forkmyfolio");

    /** All classes, including tests, for main-vs-test dependency direction checks. */
    private final JavaClasses classesWithTests = new ClassFileImporter()
            .importPackages("com.forkmyfolio");

    // ------------------------------------------------------------------
    // Controllers
    // ------------------------------------------------------------------

    @Test
    void controllers_should_not_depend_on_repositories() {
        noClasses().that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAPackage("..repository..")
                .because("Controllers orchestrate HTTP concerns and must delegate persistence " +
                        "to services (NAG-01 layering). Direct repository access bypasses the " +
                        "service boundary.")
                .check(classes);
    }

    @Test
    void controllers_should_not_call_methods_on_jpa_entities() {
        // Frozen: ~25 legacy identity/logging reads (getUuid/getId/getEmail/
        // getOwner/getToken) inside controllers stay recorded in the violation
        // store and are listed for remediation in docs/PHASE7_TRACKING.adoc;
        // any NEW entity call fails the build immediately.
        ArchRule rule = FreezingArchRule.freeze(
                noClasses().that().resideInAPackage("..controller..")
                        .should().callMethodWhere(CALL_ON_ENTITY)
                        .because("Controllers must not mutate JPA entities directly: entity mutation " +
                                "belongs in services/mappers (NAG-01). Receiving an entity from a " +
                                "service and forwarding it is not a call on the entity."));
        rule.check(classes);
    }

    @Test
    void controllers_should_not_construct_jpa_entities() {
        noClasses().that().resideInAPackage("..controller..")
                .should().callConstructorWhere(CALL_ON_ENTITY)
                .because("Entity construction belongs in mappers (DTO -> entity translation at " +
                        "the boundary) or services, not controllers (NAG-01).")
                .check(classes);
    }

    @Test
    void controllers_should_not_have_field_injection() {
        noFields().that().areDeclaredInClassesThat().resideInAPackage("..controller..")
                .should().beAnnotatedWith(AUTOWIRED)
                .orShould().beAnnotatedWith(INJECT)
                .because("Field injection is prohibited; constructor injection is the " +
                        "established pattern across all controllers and services.")
                .check(classes);
    }

    @Test
    void services_should_not_have_field_injection() {
        noFields().that().areDeclaredInClassesThat().resideInAPackage("..service..")
                .should().beAnnotatedWith(AUTOWIRED)
                .orShould().beAnnotatedWith(INJECT)
                .because("Field injection is prohibited; services use constructor injection.")
                .check(classes);
    }

    // ------------------------------------------------------------------
    // Dependency direction (NAG-01)
    // ------------------------------------------------------------------

    @Test
    void entities_should_not_depend_on_upper_layers() {
        noClasses().that().resideInAPackage("..model..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.forkmyfolio.controller..", "com.forkmyfolio.service..",
                        "com.forkmyfolio.repository..", "com.forkmyfolio.dto..",
                        "com.forkmyfolio.mapper..", "com.forkmyfolio.config..",
                        "com.forkmyfolio.security..", "com.forkmyfolio.scheduler..",
                        "com.forkmyfolio.aop..", "com.forkmyfolio.advice..",
                        "com.forkmyfolio.util..", "com.forkmyfolio.validator..")
                .because("Domain entities are the bottom of the layer stack (NAG-01). Target " +
                        "packages are scoped to com.forkmyfolio so library naming (java.util, " +
                        "org.springframework.security) can never cause false positives.")
                .check(classes);
    }

    @Test
    void repositories_should_not_depend_on_upper_layers() {
        noClasses().that().resideInAPackage("..repository..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.forkmyfolio.service..", "com.forkmyfolio.controller..",
                        "com.forkmyfolio.mapper..", "com.forkmyfolio.dto..",
                        "com.forkmyfolio.config..", "com.forkmyfolio.security..",
                        "com.forkmyfolio.scheduler..")
                .because("Repositories are a persistence abstraction; they must not reach into " +
                        "business, API or configuration layers.")
                .check(classes);
    }

    @Test
    void dtos_should_not_depend_on_services_repositories_mappers() {
        noClasses().that().resideInAPackage("..dto..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.forkmyfolio.service..", "com.forkmyfolio.repository..",
                        "com.forkmyfolio.mapper..", "com.forkmyfolio.controller..")
                .because("DTOs are inert data carriers at the API boundary (NAG-01 Rule 2).")
                .check(classes);
    }

    @Test
    void dtos_should_not_hold_entity_fields() {
        noFields().that().areDeclaredInClassesThat().resideInAPackage("..dto..")
                .should().haveRawType(ENTITY_CLASSES)
                .because("DTO fields must never expose JPA entities; only mapped DTO types and " +
                        "domain enums may appear (NAG-01 ISR/DBR).")
                .check(classes);
    }

    @Test
    void scheduled_jobs_should_not_depend_on_controllers() {
        noClasses().that().resideInAPackage("..scheduler..")
                .should().dependOnClassesThat().resideInAPackage("..controller..")
                .because("Background jobs work with services, never with the HTTP layer.")
                .check(classes);
    }

    @Test
    void advice_should_not_depend_on_repositories_or_entities() {
        noClasses().that().resideInAPackage("..advice..")
                .should().dependOnClassesThat().resideInAnyPackage("..repository..", "..model..")
                .because("Exception translation is an HTTP/API concern built on DTOs; it must " +
                        "not inspect persistence structures.")
                .check(classes);
    }

    // ------------------------------------------------------------------
    // Cycles & test leakage
    // ------------------------------------------------------------------

    @Test
    void production_code_should_be_free_of_package_cycles() {
        // Frozen: "no NEW cycles" is enforced; known legacy cycles stay
        // recorded in the violation store (archunit_store/) and are listed
        // for remediation in docs/PHASE7_TRACKING.adoc. This is ArchUnit's
        // sanctioned way to keep a rule strict while migrating legacy code -
        // the rule is not weakened.
        ArchRule rule = FreezingArchRule.freeze(
                com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices()
                        .matching("com.forkmyfolio.(*)..")
                        .should().beFreeOfCycles()
                        .as("top-level packages should be free of dependency cycles"));
        rule.check(classes);
    }

    @Test
    void test_classes_should_not_leak_into_production_code() {
        DescribedPredicate<JavaClass> productionClasses =
                new DescribedPredicate<>("production classes") {
                    @Override
                    public boolean test(JavaClass input) {
                        // Same classification as TEST_CLASSES (top-level part
                        // before '$'), so anonymous test-nested classes
                        // (e.g. ArchitectureTest$4) are never misclassified.
                        String topLevel = input.getName().split("\\$")[0];
                        return !topLevel.endsWith("Test");
                    }
                };
        noClasses().that(productionClasses)
                .should().dependOnClassesThat(TEST_CLASSES)
                .because("Production code must never depend on test classes; only the reverse " +
                        "direction is legitimate. This rule imports test classes too, so any " +
                        "'main' class reaching into a '*Test' class fails.")
                .check(classesWithTests);
    }

    // ------------------------------------------------------------------
    // Layered architecture (coarse NAG-01 flow)
    // ------------------------------------------------------------------
    // NOTE: not expressed as the library 'layeredArchitecture' DSL because the
    // security/infrastructure layers legitimately reach several layers (e.g.
    // OAuth2 success handler -> RefreshTokenService). The individual
    // directional rules above already cover the critical directionality.

    // ------------------------------------------------------------------
    // Original NAG-01 unit rules (preserved verbatim)
    // ------------------------------------------------------------------

    @Test
    void services_should_not_depend_on_dtos() {
        noClasses().that().resideInAPackage("..service..")
                .should().dependOnClassesThat().resideInAPackage("..dto..")
                .because("NAG-01 (Rule 2) forbids DTOs in the Service layer. Use Mappers at the Controller boundary.")
                .check(classes);
    }

    @Test
    void services_should_not_depend_on_controllers() {
        noClasses().that().resideInAPackage("..service..")
                .should().dependOnClassesThat().resideInAPackage("..controller..")
                .because("Services should not depend on Controllers.")
                .check(classes);
    }

    @Test
    void controllers_should_only_be_called_by_infrastructure() {
        classes().that().resideInAPackage("..controller..")
                .should().onlyHaveDependentClassesThat()
                .resideInAnyPackage("..controller..", "..config..", "..advice..", "..aop..")
                .because("Controllers are the entry point and should not be depended upon by internal business logic.")
                .check(classes);
    }
}
