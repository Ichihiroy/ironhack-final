package com.ironhack.backend.overcast.csv;

/** The uploaded file is not a parseable Azure usage-details export. */
public class CsvFormatException extends RuntimeException {
    public CsvFormatException(String message) {
        super(message);
    }
}
