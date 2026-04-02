package com.anpr.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/docs")
public class DocumentationController {

    @GetMapping("/download")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OPERATOR')")
    public ResponseEntity<byte[]> downloadHandoverGuide() throws Exception {
        Resource resource = new ClassPathResource("docs/ANPR_Handover_Guide.md");
        byte[] bytes = resource.getInputStream().readAllBytes();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/markdown; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ANPR_Handover_Guide.md")
                .contentLength(bytes.length)
                .body(new String(bytes, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8));
    }
}
