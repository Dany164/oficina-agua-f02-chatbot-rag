package com.oficinaagua.chatbot.service.monolith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Genera el documento de tarifas (Markdown) a partir de los datos públicos del monolito.
 * Es una función pura: no consulta la base de datos ni el reloj, así que se prueba con datos fijos.
 * La regla de cobro del exceso replica la del monolito (Lecturas.php): el exceso se mide en unidades
 * de 1,000 litros y cualquier fracción cuenta como una unidad completa.
 */
@Component
public class TariffDocumentGenerator {

    static final String EXCESS_TYPE_NAME = "Exceso";
    private static final int LITERS_PER_UNIT = 1000;
    private static final long[] EXAMPLE_EXCESS_LITERS = {0, 1, 5001};
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-GT"));

    public String generate(PublicCatalog catalog, LocalDate today) {
        ServiceType excessType = catalog.serviceTypes().stream()
                .filter(type -> EXCESS_TYPE_NAME.equalsIgnoreCase(type.name()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Falta el tipo de servicio '" + EXCESS_TYPE_NAME + "' en la base del monolito"));
        BigDecimal excessPrice = tariffInForce(catalog, excessType, today).amountPerUnit();

        List<ServiceType> serviceTypes = catalog.serviceTypes().stream()
                .filter(type -> type.includedLiters() != null)
                .sorted(Comparator.comparingLong(ServiceType::id))
                .toList();

        StringBuilder md = new StringBuilder();
        md.append("# Tarifas del servicio de agua\n\n");
        md.append("> Generado a partir de la base de datos de la oficina. ")
                .append("No editar a mano: se regenera con el importador.\n\n");

        md.append("## Tipos de servicio\n");
        md.append("La oficina ofrece los siguientes tipos de servicio de agua: ")
                .append(joinWithAnd(serviceTypes.stream().map(ServiceType::name).toList()))
                .append(". Todos los montos están en quetzales (Q).\n\n");

        for (ServiceType type : serviceTypes) {
            Tariff tariff = tariffInForce(catalog, type, today);
            md.append("## Tarifa del servicio ").append(type.name()).append('\n');
            md.append("La tarifa mensual del servicio ").append(type.name())
                    .append(" es de ").append(money(tariff.amountPerUnit()))
                    .append(" e incluye ").append(liters(type.includedLiters()))
                    .append(" de consumo al mes. Vigente desde el ")
                    .append(DATE_FORMAT.format(tariff.validFrom())).append(".\n\n");
        }

        md.append("## Cobro por exceso de consumo\n");
        md.append("Si el consumo del mes supera los litros incluidos en el servicio, se cobra el exceso. ")
                .append("El exceso se mide en unidades de 1,000 litros y cada fracción cuenta como una unidad ")
                .append("completa (se redondea hacia arriba). Cada unidad de exceso cuesta ")
                .append(money(excessPrice)).append(". El total a pagar es la tarifa del servicio ")
                .append("más el cobro por exceso.\n");
        for (ServiceType type : serviceTypes) {
            BigDecimal base = tariffInForce(catalog, type, today).amountPerUnit();
            md.append("\nEjemplos con el servicio ").append(type.name()).append(" (")
                    .append(liters(type.includedLiters())).append(" incluidos, tarifa ")
                    .append(money(base)).append("):\n");
            for (long excessLiters : EXAMPLE_EXCESS_LITERS) {
                md.append(exampleLine(type.includedLiters() + excessLiters, type.includedLiters(), base, excessPrice))
                        .append('\n');
            }
        }
        md.append('\n');

        List<String> methods = catalog.paymentMethods().stream()
                .map(PaymentMethod::name)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .map(TariffDocumentGenerator::describePaymentMethod)
                .toList();
        if (!methods.isEmpty()) {
            md.append("## Formas de pago\n");
            md.append("Los pagos pueden hacerse con ").append(joinWithAnd(methods)).append(".\n");
        }
        return md.toString();
    }

    /** Misma regla que el monolito: la tarifa vigente es la de vigente_desde más reciente que cubre la fecha. */
    private static Tariff tariffInForce(PublicCatalog catalog, ServiceType type, LocalDate today) {
        return catalog.tariffs().stream()
                .filter(tariff -> tariff.serviceTypeId() == type.id())
                .filter(tariff -> !tariff.validFrom().isAfter(today))
                .filter(tariff -> tariff.validTo() == null || !tariff.validTo().isBefore(today))
                .max(Comparator.comparing(Tariff::validFrom))
                .orElseThrow(() -> new IllegalStateException(
                        "No hay tarifa vigente para el tipo de servicio '" + type.name() + "' el " + today));
    }

    private static String exampleLine(long consumption, long included, BigDecimal base, BigDecimal excessPrice) {
        String prefix = "- Con " + liters(consumption) + ": ";
        long excessLiters = Math.max(0, consumption - included);
        if (excessLiters == 0) {
            return prefix + "no hay exceso; total " + money(base) + ".";
        }
        long units = (excessLiters + LITERS_PER_UNIT - 1) / LITERS_PER_UNIT;
        BigDecimal excessAmount = excessPrice.multiply(BigDecimal.valueOf(units));
        return prefix + "exceso de " + liters(excessLiters) + " = "
                + (units == 1 ? "1 unidad" : units + " unidades")
                + " x " + money(excessPrice) + " = " + money(excessAmount)
                + "; total " + money(base.add(excessAmount)) + ".";
    }

    // "Tarjeta" incluye débito y crédito: dato confirmado por el equipo del monolito (2026-10-22); no está en la base.
    private static String describePaymentMethod(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return "tarjeta".equals(lower) ? lower + " (débito o crédito)" : lower;
    }

    private static String money(BigDecimal amount) {
        return String.format(Locale.US, "Q %.2f", amount);
    }

    private static String liters(long amount) {
        return String.format(Locale.US, "%,d", amount) + (amount == 1 ? " litro" : " litros");
    }

    private static String joinWithAnd(List<String> items) {
        if (items.size() <= 1) {
            return String.join("", items);
        }
        return String.join(", ", items.subList(0, items.size() - 1)) + " y " + items.get(items.size() - 1);
    }
}