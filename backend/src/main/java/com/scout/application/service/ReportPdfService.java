package com.scout.application.service;

import com.scout.application.dto.ReportDetailDto;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportPdfService {

    private static final float MARGIN = 48;
    private static final float FONT_SIZE = 10;
    private static final float TITLE_SIZE = 16;
    private static final float LINE_HEIGHT = 14;

    public byte[] toPdf(ReportDetailDto report) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(document);
            writer.writeTitle(report.getTitle());
            writer.writeMarkdown(report.getReportMarkdown());
            writer.close();
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to generate report PDF", exception);
        }
    }

    private static class PdfWriter {
        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream contentStream;
        private float y;

        PdfWriter(PDDocument document) throws IOException {
            this.document = document;
            newPage();
        }

        void writeTitle(String title) throws IOException {
            writeLine(title, PDType1Font.HELVETICA_BOLD, TITLE_SIZE);
            blankLine();
        }

        void writeMarkdown(String markdown) throws IOException {
            for (String rawLine : markdown.split("\\R")) {
                String line = rawLine.stripTrailing();
                if (line.isBlank()) {
                    blankLine();
                    continue;
                }

                boolean heading = line.startsWith("#");
                String normalized = line
                        .replaceFirst("^#{1,6}\\s*", "")
                        .replace("**", "")
                        .replace("`", "");
                PDType1Font font = heading ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;
                float size = heading ? 12 : FONT_SIZE;
                for (String wrapped : wrap(normalized, font, size, pageWidth())) {
                    writeLine(wrapped, font, size);
                }
                if (heading) {
                    blankLine();
                }
            }
        }

        void close() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
        }

        private void newPage() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
            page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        private void writeLine(String line, PDType1Font font, float size) throws IOException {
            if (y < MARGIN) {
                newPage();
            }
            contentStream.beginText();
            contentStream.setFont(font, size);
            contentStream.newLineAtOffset(MARGIN, y);
            contentStream.showText(toPdfText(line));
            contentStream.endText();
            y -= LINE_HEIGHT;
        }

        private void blankLine() throws IOException {
            if (y < MARGIN) {
                newPage();
                return;
            }
            y -= LINE_HEIGHT;
        }

        private float pageWidth() {
            return page.getMediaBox().getWidth() - (MARGIN * 2);
        }

        private List<String> wrap(String text, PDType1Font font, float size, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            for (String word : text.split("\\s+")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (font.getStringWidth(toPdfText(candidate)) / 1000 * size <= maxWidth) {
                    current = new StringBuilder(candidate);
                } else {
                    if (!current.isEmpty()) {
                        lines.add(current.toString());
                    }
                    current = new StringBuilder(word);
                }
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
            return lines.isEmpty() ? List.of("") : lines;
        }

        private String toPdfText(String value) {
            return value
                    .replace('\u2013', '-')
                    .replace('\u2014', '-')
                    .replace('\u2018', '\'')
                    .replace('\u2019', '\'')
                    .replace('\u201c', '"')
                    .replace('\u201d', '"')
                    .replaceAll("[^\\x09\\x0A\\x0D\\x20-\\x7E]", "");
        }
    }
}
