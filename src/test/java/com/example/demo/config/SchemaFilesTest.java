package com.example.demo.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SchemaFilesTest {
    @Test void initialMigrationMatchesStandaloneSchema() throws Exception {
        assertEquals(Files.readString(Path.of("src/main/resources/db/schema.sql")),
                Files.readString(Path.of("src/main/resources/db/migration/V001__initial_schema.sql")));
    }
}
