package org.fam.dime.member;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fam.dime.member.dto.MemberResponse;

public final class MemberXlsxExporter {

	private static final String SHEET_NAME = "Membres";

	private static final int[] COLUMN_WIDTHS = { 12, 28, 28 };

	private MemberXlsxExporter() {
	}

	public static byte[] export(
			List<MemberResponse> members,
			String envelopeHeader,
			String lastNameHeader,
			String firstNameHeader) {
		try (Workbook workbook = new XSSFWorkbook();
				ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet(SHEET_NAME);
			for (int column = 0; column < COLUMN_WIDTHS.length; column++) {
				sheet.setColumnWidth(column, COLUMN_WIDTHS[column] * 256);
			}

			Row header = sheet.createRow(0);
			style(header.createCell(0), workbook);
			style(header.createCell(1), workbook);
			style(header.createCell(2), workbook);
			header.getCell(0).setCellValue(envelopeHeader);
			header.getCell(1).setCellValue(lastNameHeader);
			header.getCell(2).setCellValue(firstNameHeader);

			int rowIndex = 1;
			for (MemberResponse member : members) {
				Row row = sheet.createRow(rowIndex++);
				row.createCell(0).setCellValue(member.envelopeNumber() == null ? 0 : member.envelopeNumber().doubleValue());
				row.createCell(1).setCellValue(member.lastName() == null ? "" : member.lastName());
				row.createCell(2).setCellValue(member.firstName() == null ? "" : member.firstName());
			}

			workbook.write(output);
			return output.toByteArray();
		} catch (IOException exception) {
			throw new IllegalStateException("Cannot export members to xlsx", exception);
		}
	}

	private static void style(Cell cell, Workbook workbook) {
		Font font = workbook.createFont();
		font.setBold(true);
		CellStyle style = workbook.createCellStyle();
		style.setFont(font);
		cell.setCellStyle(style);
	}
}