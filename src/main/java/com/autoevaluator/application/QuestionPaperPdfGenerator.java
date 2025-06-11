package com.autoevaluator.application;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.canvas.draw.DashedLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.io.image.ImageDataFactory;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class QuestionPaperPdfGenerator {
    public static byte[] generateQuestionPaper(
            String collegeName,
            String logoPath,
            String courseName,
            String courseCode,
            String departmentName,
            int semester,
            String paperType,
            List<String> questions,
            String examDate,
            String examTime) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Header: College name and logo
        float[] columnWidths = {1, 0.25f};
        Table headerTable = new Table(columnWidths);
        headerTable.setWidth(500);

        // Left cell
        Paragraph left = new Paragraph()
                .add("NATIONAL INSTITUTE OF TECHNOLOGY SRINAGAR" + "\n")
                .add("DEPARTMENT OF " + departmentName.toUpperCase() + "\n")
                .setFontSize(10)
                .setBold();

        Cell leftCell = new Cell().add(left)
                .setBorder(null)
                .setTextAlignment(TextAlignment.LEFT)
                .setPadding(20);
        headerTable.addCell(leftCell);

        // Right cell (logo)
        Cell rightCell = new Cell().setBorder(null)
                .setTextAlignment(TextAlignment.RIGHT)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(0);

        if (logoPath != null && !logoPath.isEmpty()) {
            Image logo = new Image(ImageDataFactory.create(logoPath));
            logo.setHeight(150);
            logo.setAutoScale(true);
            rightCell.add(logo);
        }
        headerTable.addCell(rightCell);

        document.add(headerTable);
        document.add(new LineSeparator(new DashedLine(1)));

        // Paper Info Block
        Paragraph paperInfo = new Paragraph()
                .add("Course: " + courseName + " (" + courseCode + ")\n")
                .add("Semester: " + semester + "\n")
                .add("Paper Type: " + paperType + "\n")
                .add("Exam Date: " + examDate + "\n")
                .add("Exam Time: " + examTime + "\n")
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10)
                .setMarginBottom(10)
                .setFontSize(11);
        document.add(paperInfo);

        document.add(new LineSeparator(new DashedLine(1)));

        // Questions Section
        if (questions != null && !questions.isEmpty()) {
            int count = 1;
            for (String q : questions) {
                if (q == null || q.trim().isEmpty()) q = "[No question text]";
                int marksIndex = q.lastIndexOf("(Marks:");
                String leftPart = (marksIndex > 0) ? q.substring(0, marksIndex).trim() : q;
                String marksPart = (marksIndex > 0) ? q.substring(marksIndex).trim() : "";

                Paragraph question = new Paragraph("Q" + count + ". " + leftPart + "       " + marksPart)
                        .setBold()
                        .setMarginBottom(5);
                document.add(question);
                count++;
            }
        }

        document.close();
        return baos.toByteArray();
    }
}
