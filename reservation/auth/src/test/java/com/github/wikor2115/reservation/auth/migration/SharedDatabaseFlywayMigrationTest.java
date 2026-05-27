package com.github.wikor2115.reservation.auth.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SharedDatabaseFlywayMigrationTest {

    private static final List<ModuleMigration> MODULES = List.of(
            new ModuleMigration("auth", Path.of("."), List.of("auth_user")),
            new ModuleMigration("offer", Path.of("../offer"), List.of("offer")),
            new ModuleMigration("availability", Path.of("../availability"), List.of("availability_slot")),
            new ModuleMigration("booking", Path.of("../booking"), List.of("reservation"))
    );

    @TempDir
    Path tempDir;

    @Test
    void appliesAllModuleMigrationsToOneSharedSchema() throws SQLException, IOException {
        String jdbcUrl = "jdbc:h2:file:" + tempDir.resolve("reservation")
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

        for (ModuleMigration module : MODULES) {
            Properties application = loadProperties(module.modulePath()
                    .resolve("src/main/resources/application.properties"));
            Properties postgres = loadProperties(module.modulePath()
                    .resolve("src/main/resources/application-dev-postgres.properties"));

            assertThat(application.getProperty("spring.flyway.baseline-on-migrate"))
                    .isEqualTo("true");
            assertThat(application.getProperty("spring.flyway.baseline-version"))
                    .isEqualTo("0");
            assertThat(postgres.getProperty("spring.flyway.baseline-on-migrate"))
                    .isEqualTo("true");
            assertThat(postgres.getProperty("spring.flyway.baseline-version"))
                    .isEqualTo("0");

            String location = application.getProperty("spring.flyway.locations");
            assertThat(location).isEqualTo("classpath:db/migration/" + module.name());

            Flyway.configure()
                    .dataSource(jdbcUrl, "sa", "")
                    .locations(toFilesystemLocation(module.modulePath(), location))
                    .table(postgres.getProperty("spring.flyway.table"))
                    .baselineOnMigrate(Boolean.parseBoolean(postgres.getProperty("spring.flyway.baseline-on-migrate")))
                    .baselineVersion(postgres.getProperty("spring.flyway.baseline-version"))
                    .load()
                    .migrate();
        }

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            for (ModuleMigration module : MODULES) {
                assertTableExists(connection, module.name() + "_flyway_schema_history");
                for (String table : module.tables()) {
                    assertTableExists(connection, table);
                }
            }
        }
    }

    private static Properties loadProperties(Path path) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = path.toRealPath().toUri().toURL().openStream()) {
            properties.load(input);
        }
        return properties;
    }

    private static String toFilesystemLocation(Path modulePath, String classpathLocation) {
        String resourcePath = classpathLocation.replace("classpath:", "src/main/resources/");
        return "filesystem:" + modulePath.resolve(resourcePath).toAbsolutePath().normalize();
    }

    private static void assertTableExists(Connection connection, String tableName) throws SQLException {
        String normalizedName = tableName.toLowerCase(Locale.ROOT);
        try (var statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = 'PUBLIC'
                    AND LOWER(TABLE_NAME) = ?
                """)) {
            statement.setString(1, normalizedName);
            try (var resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt(1)).isEqualTo(1);
            }
        }
    }

    private record ModuleMigration(String name, Path modulePath, List<String> tables) {
    }
}
