package org.fam.dime.importer.dto;

public record ImportRowErrorResponse(int lineNumber, String rawLine, String message) {
}