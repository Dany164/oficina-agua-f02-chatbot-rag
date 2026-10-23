package com.oficinaagua.chatbot.service.monolith;

/** Fila de Tb_Tipos_Servicio. Un tipo sin litros incluidos (el "Exceso") no es un servicio contratable. */
public record ServiceType(long id, String name, Integer includedLiters) {
}
