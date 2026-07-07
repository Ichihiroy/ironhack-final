package com.ironhack.backend.overcast.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal RFC 4180 CSV reader (quoted fields, embedded commas/quotes/newlines).
 * Hand-rolled on purpose: no extra dependency, fully deterministic, ~80 lines.
 */
public final class CsvReader {

    private CsvReader() {}

    /** Parses the whole input; first row is returned as-is (headers included). */
    public static List<List<String>> parse(Reader input) throws IOException {
        BufferedReader reader = input instanceof BufferedReader br ? br : new BufferedReader(input);
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean anyChar = false;

        int c;
        while ((c = reader.read()) != -1) {
            char ch = (char) c;
            if (inQuotes) {
                if (ch == '"') {
                    int next = reader.read();
                    if (next == '"') {
                        field.append('"'); // escaped quote
                    } else {
                        inQuotes = false;
                        if (next == -1) break;
                        ch = (char) next;
                        // fall through to unquoted handling of the char after the closing quote
                        if (ch == ',') {
                            row.add(field.toString());
                            field.setLength(0);
                        } else if (ch == '\n' || ch == '\r') {
                            // a following \n after \r is skipped by the blank-row guard
                            endRow(rows, row, field);
                            anyChar = false;
                            continue;
                        } else {
                            field.append(ch);
                        }
                    }
                } else {
                    field.append(ch);
                }
                anyChar = true;
            } else {
                switch (ch) {
                    case '"' -> {
                        inQuotes = true;
                        anyChar = true;
                    }
                    case ',' -> {
                        row.add(field.toString());
                        field.setLength(0);
                        anyChar = true;
                    }
                    case '\r' -> { /* handled with the following \n (or as bare CR below) */
                        endRowIfAny(rows, row, field, anyChar);
                        anyChar = false;
                    }
                    case '\n' -> {
                        endRowIfAny(rows, row, field, anyChar);
                        anyChar = false;
                    }
                    default -> {
                        field.append(ch);
                        anyChar = true;
                    }
                }
            }
        }
        endRowIfAny(rows, row, field, anyChar || !row.isEmpty() || field.length() > 0);
        return rows;
    }

    private static void endRowIfAny(List<List<String>> rows, List<String> row, StringBuilder field, boolean anyChar) {
        if (!anyChar && row.isEmpty() && field.length() == 0) return; // blank line / trailing newline
        endRow(rows, row, field);
    }

    private static void endRow(List<List<String>> rows, List<String> row, StringBuilder field) {
        row.add(field.toString());
        field.setLength(0);
        rows.add(new ArrayList<>(row));
        row.clear();
    }
}
