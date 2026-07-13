package com.app.backend.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal streaming RFC-4180 CSV reader for the VZD address register files:
 * comma-delimited, optionally quoted fields with {@code ""} escapes, UTF-8 with
 * a BOM, first record is the header. Rows are streamed one at a time so the
 * large building file never has to fit in memory.
 */
public final class CsvStream implements AutoCloseable {

    private final Reader reader;
    private final Map<String, Integer> columns;

    private CsvStream(Reader reader, Map<String, Integer> columns) {
        this.reader = reader;
        this.columns = columns;
    }

    public static CsvStream open(InputStream in) throws IOException {
        Reader reader = new BufferedReader(new java.io.InputStreamReader(in, StandardCharsets.UTF_8), 1 << 16);
        CsvStream csv = new CsvStream(reader, new HashMap<>());
        String[] header = csv.next();
        if (header == null) {
            throw new IOException("CSV file is empty");
        }
        for (int i = 0; i < header.length; i++) {
            String name = i == 0 ? stripBom(header[i]) : header[i];
            csv.columns.put(name, i);
        }
        return csv;
    }

    /** The next record's fields, or null at end of input. */
    public String[] next() throws IOException {
        int c = reader.read();
        if (c == -1) {
            return null;
        }
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        while (c != -1) {
            if (quoted) {
                if (c == '"') {
                    int peek = reader.read();
                    if (peek == '"') {
                        field.append('"');
                    } else {
                        quoted = false;
                        c = peek;
                        continue;
                    }
                } else {
                    field.append((char) c);
                }
            } else if (c == '"' && field.isEmpty()) {
                quoted = true;
            } else if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
            } else if (c == '\n') {
                break;
            } else if (c != '\r') {
                field.append((char) c);
            }
            c = reader.read();
        }
        fields.add(field.toString());
        return fields.toArray(String[]::new);
    }

    /** The named column's value in a record, or "" when the record is short. */
    public String col(String[] record, String name) {
        Integer idx = columns.get(name);
        if (idx == null) {
            throw new IllegalArgumentException("CSV has no column named " + name);
        }
        return idx < record.length ? record[idx] : "";
    }

    private static String stripBom(String s) {
        return s.startsWith("\uFEFF") ? s.substring(1) : s;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
