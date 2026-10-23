package com.oficinaagua.chatbot.service.monolith;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Fila de Tb_Tarifas. validTo nulo significa que la tarifa sigue vigente. */
public record Tariff(long id, BigDecimal amountPerUnit, LocalDate validFrom, LocalDate validTo, long serviceTypeId) {
}
