package com.plagiarism.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Extracts plain text from various document formats:
 * - Word (.docx): Parses XML structure inside docx zip container
 * - Text / Markdown / CSV / JSON: Decodes UTF-8 / ASCII strings
 */
public class DocumentExtractor {

    /**
     * Extracts text from Word (.docx) file bytes without third-party heavy dependencies.
     */
    public static String extractFromDocx(byte[] bytes) {
        StringBuilder text = new StringBuilder();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("word/document.xml".equalsIgnoreCase(entry.getName())) {
                    byte[] buffer = zis.readAllBytes();
                    String xml = new String(buffer, StandardCharsets.UTF_8);

                    // Extract all <w:t>...</w:t> tags
                    Pattern pattern = Pattern.compile("<w:t[^>]*>(.*?)</w:t>");
                    Matcher matcher = pattern.matcher(xml);
                    while (matcher.find()) {
                        text.append(matcher.group(1)).append(" ");
                    }

                    // Add line breaks for paragraph ends <w:p>
                    return text.toString()
                            .replace("&amp;", "&")
                            .replace("&lt;", "<")
                            .replace("&gt;", ">")
                            .replace("&quot;", "\"")
                            .replace("&apos;", "'")
                            .trim();
                }
            }
        } catch (Exception e) {
            System.err.println("DOCX extraction error: " + e.getMessage());
        }
        return text.toString();
    }

    /**
     * Extracts plain text from raw PDF bytes (stream text parser fallback).
     */
    public static String extractFromPdfRaw(byte[] bytes) {
        StringBuilder text = new StringBuilder();
        try {
            String raw = new String(bytes, StandardCharsets.ISO_8859_1);
            // Match text inside BT (Begin Text) and ET (End Text) blocks with (string) Tj or TJ
            Pattern pattern = Pattern.compile("\\(([^\\)]+)\\)\\s*(?:Tj|'|\")");
            Matcher matcher = pattern.matcher(raw);
            while (matcher.find()) {
                text.append(matcher.group(1)).append(" ");
            }
        } catch (Exception e) {
            System.err.println("PDF raw extraction error: " + e.getMessage());
        }
        return text.toString().trim();
    }
}
