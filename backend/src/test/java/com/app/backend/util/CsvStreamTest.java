package com.app.backend.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvStreamTest {

    private static CsvStream open(String content) throws IOException {
        InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        return CsvStream.open(in);
    }

    @Test
    void parsesQuotedFieldsWithEscapedQuotesAndCommas() throws IOException {
        try (CsvStream csv = open("""
                KODS,NOSAUKUMS,STD
                "1","Riņņi","\"\"Riņņi\"\", Vecates pag., Valmieras nov."
                """)) {
            String[] row = csv.next();
            assertThat(csv.col(row, "KODS")).isEqualTo("1");
            assertThat(csv.col(row, "NOSAUKUMS")).isEqualTo("Riņņi");
            assertThat(csv.col(row, "STD")).isEqualTo("\"Riņņi\", Vecates pag., Valmieras nov.");
            assertThat(csv.next()).isNull();
        }
    }

    @Test
    void stripsBomAndHandlesCrlf() throws IOException {
        try (CsvStream csv = open("\uFEFFA,B\r\n\"x\",\"y\"\r\n")) {
            String[] row = csv.next();
            assertThat(csv.col(row, "A")).isEqualTo("x");
            assertThat(csv.col(row, "B")).isEqualTo("y");
        }
    }

    @Test
    void handlesUnquotedFieldsAndMissingTrailingNewline() throws IOException {
        try (CsvStream csv = open("A,B\n1,2")) {
            String[] row = csv.next();
            assertThat(row).containsExactly("1", "2");
            assertThat(csv.next()).isNull();
        }
    }

    @Test
    void preservesNewlineInsideQuotedField() throws IOException {
        try (CsvStream csv = open("A,B\n\"line1\nline2\",z\n")) {
            String[] row = csv.next();
            assertThat(csv.col(row, "A")).isEqualTo("line1\nline2");
            assertThat(csv.col(row, "B")).isEqualTo("z");
        }
    }

    @Test
    void colThrowsOnUnknownColumnAndReturnsEmptyOnShortRecord() throws IOException {
        try (CsvStream csv = open("A,B\n1\n")) {
            String[] row = csv.next();
            assertThat(csv.col(row, "B")).isEmpty();
            assertThatThrownBy(() -> csv.col(row, "MISSING"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
