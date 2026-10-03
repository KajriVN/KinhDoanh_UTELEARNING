package com.utelearn.controller;

import com.utelearn.entity.Certificate;
import com.utelearn.service.CertificateService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Tra cứu / tải chứng chỉ. Công khai: ai có mã chứng chỉ đều xác thực được (nhà tuyển dụng, đối tác...).
@Controller
public class CertificateController {

    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @GetMapping("/certificates/{code}")
    public String view(@PathVariable String code, Model model) {
        model.addAttribute("certificate", certificateService.findByCode(code));
        return "certificate/view";
    }

    @GetMapping("/certificates/{code}/download")
    public ResponseEntity<byte[]> download(@PathVariable String code) {
        Certificate certificate = certificateService.findByCode(code);
        byte[] pdf = certificateService.buildPdf(certificate);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + certificate.getCode() + ".pdf\"")
                .body(pdf);
    }
}
