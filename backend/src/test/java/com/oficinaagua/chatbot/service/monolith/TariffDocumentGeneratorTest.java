package com.oficinaagua.chatbot.service.monolith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oficinaagua.chatbot.service.ingestion.MarkdownSection;
import com.oficinaagua.chatbot.service.ingestion.MarkdownSectionSplitter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Los datos de prueba son las filas reales de las tablas públicas del monolito (consultadas el 2026-10-22).
 */
class TariffDocumentGeneratorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);

    private final TariffDocumentGenerator generator = new TariffDocumentGenerator();

    @Test
    void generatesTheFixedSectionsAndOneTariffSectionPerServiceType() {
        String markdown = generator.generate(realCatalog(), TODAY);

        List<MarkdownSection> sections = new MarkdownSectionSplitter().split(markdown);

        assertThat(sections).extracting(MarkdownSection::heading).containsExactly(
                "Tipos de servicio",
                "Tarifa del servicio 1/4 paja",
                "Tarifa del servicio 1/2 paja",
                "Cobro por exceso de consumo",
                "Formas de pago");
    }

    @Test
    void usesTheTariffsInForceToday() {
        String markdown = generator.generate(realCatalog(), TODAY);

        assertThat(markdown).contains("Q 75.00", "Q 150.00", "Q 5.00")
                .doesNotContain("Q 60.00", "Q 120.00", "Q 4.00");
    }

    @Test
    void usesThePreviousTariffsUpToTheirLastValidDay() {
        String markdown = generator.generate(realCatalog(), LocalDate.of(2026, 5, 11));

        assertThat(markdown).contains("Q 60.00", "Q 120.00", "Q 4.00").doesNotContain("Q 75.00");
    }

    @Test
    void switchesToTheNewTariffsOnTheirFirstValidDay() {
        String markdown = generator.generate(realCatalog(), LocalDate.of(2026, 5, 12));

        assertThat(markdown).contains("Q 75.00").doesNotContain("Q 60.00");
    }

    @Test
    void describesIncludedLitersWithThousandsSeparatorAndTheValidityDate() {
        String markdown = generator.generate(realCatalog(), TODAY);

        assertThat(markdown).contains(
                "incluye 15,000 litros de consumo al mes. Vigente desde el 12 de mayo de 2026.",
                "incluye 60,000 litros de consumo al mes. Vigente desde el 12 de mayo de 2026.");
    }

    @Test
    void explainsTheExcessAsUnitsOfOneThousandLitersRoundedUp() {
        String markdown = generator.generate(realCatalog(), TODAY);

        assertThat(markdown).contains(
                "unidades de 1,000 litros",
                "se redondea hacia arriba",
                "Cada unidad de exceso cuesta Q 5.00");
    }

    @Test
    void chargesNoExcessAtTheIncludedLimit() {
        String markdown = generator.generate(realCatalog(), TODAY);

        assertThat(markdown).contains("- Con 15,000 litros: no hay exceso; total Q 75.00.");
    }

    @Test
    void roundsAnyFractionOfAThousandLitersUpToOneUnit() {
        String markdown = generator.generate(realCatalog(), TODAY);

        assertThat(markdown).contains(
                "- Con 15,001 litros: exceso de 1 litro = 1 unidad x Q 5.00 = Q 5.00; total Q 80.00.");
    }

    @Test
    void countsFiveThousandAndOneExcessLitersAsSixUnits() {
        String markdown = generator.generate(realCatalog(), TODAY);

        assertThat(markdown).contains(
                "- Con 20,001 litros: exceso de 5,001 litros = 6 unidades x Q 5.00 = Q 30.00; total Q 105.00.",
                "- Con 65,001 litros: exceso de 5,001 litros = 6 unidades x Q 5.00 = Q 30.00; total Q 180.00.");
    }

    @Test
    void listsThePaymentMethodsAndNotesThatCardsCoverDebitAndCredit() {
        String markdown = generator.generate(realCatalog(), TODAY);

        assertThat(markdown).contains(
                "Los pagos pueden hacerse con efectivo, tarjeta (débito o crédito) y transferencia.");
    }

    @Test
    void failsWhenThereIsNoExcessServiceType() {
        PublicCatalog catalog = realCatalog();
        List<ServiceType> withoutExcess = catalog.serviceTypes().stream()
                .filter(type -> type.includedLiters() != null).toList();
        PublicCatalog broken = new PublicCatalog(withoutExcess, catalog.tariffs(), catalog.paymentMethods());

        assertThatThrownBy(() -> generator.generate(broken, TODAY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Exceso");
    }

    @Test
    void failsWhenAServiceTypeHasNoTariffInForce() {
        PublicCatalog catalog = realCatalog();
        List<Tariff> withoutQuarterPaja = catalog.tariffs().stream()
                .filter(tariff -> tariff.serviceTypeId() != 1).toList();
        PublicCatalog broken = new PublicCatalog(catalog.serviceTypes(), withoutQuarterPaja, catalog.paymentMethods());

        assertThatThrownBy(() -> generator.generate(broken, TODAY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("1/4 paja");
    }

    private static PublicCatalog realCatalog() {
        List<ServiceType> types = List.of(
                new ServiceType(1, "1/4 paja", 15000),
                new ServiceType(2, "1/2 paja", 60000),
                new ServiceType(3, "Exceso", null));
        List<Tariff> tariffs = List.of(
                new Tariff(1, new BigDecimal("60.00"), LocalDate.of(2025, 9, 12), LocalDate.of(2026, 5, 11), 1),
                new Tariff(2, new BigDecimal("75.00"), LocalDate.of(2026, 5, 12), null, 1),
                new Tariff(3, new BigDecimal("120.00"), LocalDate.of(2025, 9, 12), LocalDate.of(2026, 5, 11), 2),
                new Tariff(4, new BigDecimal("150.00"), LocalDate.of(2026, 5, 12), null, 2),
                new Tariff(5, new BigDecimal("4.00"), LocalDate.of(2025, 9, 12), LocalDate.of(2026, 5, 11), 3),
                new Tariff(6, new BigDecimal("5.00"), LocalDate.of(2026, 5, 12), null, 3));
        List<PaymentMethod> methods = List.of(
                new PaymentMethod(1, "Efectivo"),
                new PaymentMethod(3, "Tarjeta"),
                new PaymentMethod(2, "Transferencia"));
        return new PublicCatalog(types, tariffs, methods);
    }
}