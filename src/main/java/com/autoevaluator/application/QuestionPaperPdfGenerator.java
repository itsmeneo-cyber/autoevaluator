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

        // Header
        float[] columnWidths = {1, 0.25f};
        Table headerTable = new Table(columnWidths);
        headerTable.setWidth(500);

        Paragraph left = new Paragraph()
                .add("NATIONAL INSTITUTE OF TECHNOLOGY SRINAGAR\n")
                .add("DEPARTMENT OF " + departmentName.toUpperCase() + "\n")
                .setFontSize(10)
                .setBold();

        Cell leftCell = new Cell().add(left)
                .setBorder(null)
                .setTextAlignment(TextAlignment.LEFT)
                .setPadding(20);
        headerTable.addCell(leftCell);

        Cell rightCell = new Cell().setBorder(null)
                .setTextAlignment(TextAlignment.RIGHT)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(0);


        headerTable.addCell(rightCell);

        document.add(headerTable);
        document.add(new LineSeparator(new DashedLine(1)));

        // Paper Info
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

        // Questions
        if (questions != null && !questions.isEmpty()) {
            int count = 1;
            for (String q : questions) {
                if (q == null || q.trim().isEmpty()) q = "[No question text]";

                int marksIndex = q.lastIndexOf("(Marks:");
                String leftPart = (marksIndex > 0) ? q.substring(0, marksIndex).trim() : q;
                String marksPart = (marksIndex > 0) ? q.substring(marksIndex).trim() : "";

                // Extract instructions
                String instructions = "";
                int instrIndex = leftPart.lastIndexOf(" - Instructions:");
                if (instrIndex != -1) {
                    instructions = leftPart.substring(instrIndex + " - Instructions:".length()).trim();
                    leftPart = leftPart.substring(0, instrIndex).trim();
                }

                // Bold question line
                Paragraph questionPara = new Paragraph()
                        .add(new Text("Q" + count + ". " + leftPart + " " + marksPart).setBold())
                        .setMarginBottom(instructions.isEmpty() ? 20 : 5); // more gap before next block
                document.add(questionPara);

                // Italic instructions if any
                if (!instructions.isEmpty()) {
                    Paragraph instrPara = new Paragraph()
                            .add(new Text("Instructions: " + instructions).setItalic())
                            .setFontSize(10)
                            .setMarginBottom(15); // spacing after instructions
                    document.add(instrPara);
                }

                count++;
            }
        }

        document.close();
        System.out.println(examTime);
        return baos.toByteArray();
    }
}
