package com.oficinaagua.chatbot.repository;

import com.oficinaagua.chatbot.service.monolith.PaymentMethod;
import com.oficinaagua.chatbot.service.monolith.PublicCatalog;
import com.oficinaagua.chatbot.service.monolith.ServiceType;
import com.oficinaagua.chatbot.service.monolith.Tariff;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Lee del monolito (solo lectura) los datos públicos que el chatbot puede explicar:
 * tipos de servicio, tarifas y métodos de pago. Usa JDBC simple y TLS con verificación
 * del servidor. Solo existe con el perfil "import-monolith".
 */
@Component
@Profile("import-monolith")
public class MonolithCatalogRepository {

    private final String url;
    private final String user;
    private final String password;

    public MonolithCatalogRepository(@Value("${chatbot.monolith.host}") String host,
                                     @Value("${chatbot.monolith.port}") int port,
                                     @Value("${chatbot.monolith.name}") String database,
                                     @Value("${chatbot.monolith.user}") String user,
                                     @Value("${chatbot.monolith.password}") String password,
                                     @Value("${chatbot.monolith.ca-cert}") String caCert) {
        this.url = jdbcUrl(host, port, database, Path.of(caCert));
        this.user = user;
        this.password = password;
    }

    static String jdbcUrl(String host, int port, String database, Path caCert) {
        String certPath = caCert.toAbsolutePath().normalize().toString().replace('\\', '/');
        return "jdbc:mariadb://" + host + ":" + port + "/" + database
                + "?sslMode=verify-full&serverSslCert=" + certPath;
    }

    public PublicCatalog load() {
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            connection.setReadOnly(true);
            return new PublicCatalog(
                    serviceTypes(connection), tariffs(connection), paymentMethods(connection));
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo leer el catálogo público del monolito: " + e.getMessage(), e);
        }
    }

    private static List<ServiceType> serviceTypes(Connection connection) throws SQLException {
        List<ServiceType> result = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT tipo_servicio_id, tipo_servicio, litros_incluidos FROM Tb_Tipos_Servicio")) {
            while (rs.next()) {
                int liters = rs.getInt("litros_incluidos");
                Integer includedLiters = rs.wasNull() ? null : liters;
                result.add(new ServiceType(
                        rs.getLong("tipo_servicio_id"), rs.getString("tipo_servicio"), includedLiters));
            }
        }
        return result;
    }

    private static List<Tariff> tariffs(Connection connection) throws SQLException {
        List<Tariff> result = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT tarifa_id, monto_por_unidad, vigente_desde, vigente_hasta, tipo_servicio_id"
                             + " FROM Tb_Tarifas")) {
            while (rs.next()) {
                Date validTo = rs.getDate("vigente_hasta");
                result.add(new Tariff(
                        rs.getLong("tarifa_id"),
                        rs.getBigDecimal("monto_por_unidad"),
                        rs.getDate("vigente_desde").toLocalDate(),
                        validTo == null ? null : validTo.toLocalDate(),
                        rs.getLong("tipo_servicio_id")));
            }
        }
        return result;
    }

    private static List<PaymentMethod> paymentMethods(Connection connection) throws SQLException {
        List<PaymentMethod> result = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT metodos_pago_id, metodo FROM Tb_Metodos_Pago")) {
            while (rs.next()) {
                result.add(new PaymentMethod(rs.getLong("metodos_pago_id"), rs.getString("metodo")));
            }
        }
        return result;
    }
}