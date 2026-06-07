package com.example.kproject.exception;

public class ReportAccessDeniedException extends RuntimeException {

    public ReportAccessDeniedException(Long reportId) {
        super("Report access denied: " + reportId);
    }
}
