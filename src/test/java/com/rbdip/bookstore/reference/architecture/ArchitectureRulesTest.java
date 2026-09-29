package com.rbdip.bookstore.reference.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

/**
 * Архитектурный гейт ЛР4: модуль review не должен напрямую зависеть от
 * пакета order. На старте (до рефакторинга) этот тест КРАСНЫЙ - это
 * ожидаемо и документирует смелл, который нужно устранить, выделив
 * review в независимый модуль (Strangler Fig). Тест не редактируется
 * студентами: правило - часть задания, а не то, что подгоняется под код.
 */
class ArchitectureRulesTest {

    private static final com.tngtech.archunit.core.domain.JavaClasses IMPORTED_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.rbdip.bookstore");

    @Test
    void reviewPackageMustNotDependOnOrderPackage() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.rbdip.bookstore.review..")
                .should().dependOnClassesThat().resideInAPackage("com.rbdip.bookstore.order..");

        rule.check(IMPORTED_CLASSES);
    }
}
