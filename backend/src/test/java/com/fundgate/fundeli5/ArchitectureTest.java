package com.fundgate.fundeli5;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Architecture guards of tech-choices.md. Packages may be empty: the rules wait for the code. */
@AnalyzeClasses(packages = "com.fundgate.fundeli5", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String ROOT = "com.fundgate.fundeli5";

    /** 1. ADR 0002: no LLM at check time. */
    @ArchTest
    static final ArchRule ruleEngineNeverDependsOnLlm = noClasses()
            .that().resideInAPackage("..ruleengine..")
            .should().dependOnClassesThat().resideInAPackage("..llm..")
            .allowEmptyShould(true);

    /** ADR 0004: the egress client is the only code that can reach a model over the network. */
    @ArchTest
    static final ArchRule onlyTheEgressReachesTheNetwork = noClasses()
            .that().resideOutsideOfPackages("..llm..", "..web..", ROOT)
            .and().resideOutsideOfPackages("..fixture..", "..folder..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.anthropic..", "java.net.http..", "org.springframework.web.client..",
                    "org.springframework.web.reactive.function.client..")
            .allowEmptyShould(true);

    /** 2. Only the Privacy Gate builds its output type. */
    @ArchTest
    static final ArchRule onlyPrivacyGateConstructsGatedEmail = noClasses()
            .that().resideOutsideOfPackage("..privacygate..")
            .should().callConstructorWhere(target(owner(simpleName("GatedEmail"))))
            .allowEmptyShould(true);

    /** 3. ADR 0004: the LLM-facing modules see Portfolio Ids only. */
    @ArchTest
    static final ArchRule llmFacingModulesNeverSeePortfolioReferences = noClasses()
            .that().resideInAnyPackage("..requestparser..", "..replydrafter..")
            .should().dependOnClassesThat(simpleName("PortfolioReference").or(simpleName("GatedEmail")))
            .allowEmptyShould(true);

    /** 4. Spring lives in the root wiring package and the web layer only. */
    @ArchTest
    static final ArchRule springOnlyInWiringAndWeb = noClasses()
            .that().resideOutsideOfPackages(ROOT, "..web..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..")
            .allowEmptyShould(true);

    /** 5. The card is data; the engine reads it, never the other way. */
    @ArchTest
    static final ArchRule ruleCardNeverDependsOnRuleEngine = noClasses()
            .that().resideInAPackage("..rulecard..")
            .should().dependOnClassesThat().resideInAPackage("..ruleengine..")
            .allowEmptyShould(true);

    /** 6a. A rule kind's Condition is used only by its plug-in (and the card that carries it). */
    @ArchTest
    static final ArchRule conditionsAreUsedOnlyByPlugIns = noClasses()
            .that().resideOutsideOfPackages("..ruleengine.plugin..", "..rulecard..")
            .should().dependOnClassesThat(describe("a concrete Condition",
                    c -> c.isAssignableTo("com.fundgate.fundeli5.rulecard.Condition")
                            && !c.getName().equals("com.fundgate.fundeli5.rulecard.Condition")))
            .allowEmptyShould(true);

    /** 6b. Only the plug-ins name a kind of rule. */
    @ArchTest
    static final ArchRule onlyPlugInsNameARuleKind = noClasses()
            .that().resideOutsideOfPackages("..ruleengine.plugin..", "..rulecard..")
            .should().accessFieldWhere(com.tngtech.archunit.core.domain.JavaFieldAccess.Predicates.target(
                    owner(describe("RuleKind", c -> c.getName().equals("com.fundgate.fundeli5.rulecard.RuleKind")))))
            .allowEmptyShould(true);

    /** Domain classes import no Spring, Jackson or PDFBox; only the adapters, readers and wiring do. */
    @ArchTest
    static final ArchRule domainClassesImportNoFrameworks = noClasses()
            .that().resideInAnyPackage("..eligibility..", "..rulecard..", "..citation..", "..bankdata..",
                    "..clientprofiles..", "..shareclasses..", "..funddocuments..", "..fundregistrations..", "..fxrates..")
            .and().resideOutsideOfPackages("..fixture..", "..folder..", "..json..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "tools.jackson..", "com.fasterxml.jackson..", "org.apache.pdfbox..")
            .allowEmptyShould(true);
}
