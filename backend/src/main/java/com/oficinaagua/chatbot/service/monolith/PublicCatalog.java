package com.oficinaagua.chatbot.service.monolith;

import java.util.List;

/** Datos públicos del monolito: las únicas tablas que el usuario de solo lectura puede consultar. */
public record PublicCatalog(List<ServiceType> serviceTypes, List<Tariff> tariffs, List<PaymentMethod> paymentMethods) {
}
