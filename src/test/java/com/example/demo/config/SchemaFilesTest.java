package com.example.demo.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SchemaFilesTest {
    @Test void initialMigrationMatchesStandaloneSchema() throws Exception {
        // Khoảng trắng ngoài cùng / kiểu xuống dòng không thay đổi nội dung SQL.
        assertEquals(normalize(Files.readString(Path.of("src/main/resources/db/schema.sql"))),
                normalize(Files.readString(Path.of("src/main/resources/db/migration/V001__initial_schema.sql"))));
    }
    private static String normalize(String sql) { return sql.replace("\r\n", "\n").strip(); }
}
