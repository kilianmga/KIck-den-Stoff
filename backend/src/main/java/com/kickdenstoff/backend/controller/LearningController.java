package com.kickdenstoff.backend.controller;

import java.util.Map;

import com.kickdenstoff.backend.config.LmStudioProperties;
import com.kickdenstoff.backend.dto.LearningMode;
import com.kickdenstoff.backend.dto.LearningRequest;
import com.kickdenstoff.backend.dto.LearningResponse;
import com.kickdenstoff.backend.service.LearningService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api")
public class LearningController {

    private final LearningService learningService;
    private final LmStudioProperties lmStudioProperties;

    public LearningController(LearningService learningService, LmStudioProperties lmStudioProperties) {
        this.learningService = learningService;
        this.lmStudioProperties = lmStudioProperties;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "ok",
                "service", "kick-den-stoff-backend",
                "lmStudioBaseUrl", lmStudioProperties.baseUrl(),
                "model", lmStudioProperties.model()
        );
    }

    @PostMapping(value = "/learn", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LearningResponse learn(@Valid @RequestBody LearningRequest request) {
        return learningService.learn(LearningMode.from(request.mode()), request);
    }

    @PostMapping(value = "/learn/{mode}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LearningResponse learnWithPdf(
            @PathVariable String mode,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String gradeLevel,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String inputText,
            @RequestParam(required = false) String input,
            @RequestParam(required = false) String userQuestion,
            @RequestParam(required = false) String style,
            @RequestParam("pdf") MultipartFile pdfFile
    ) {
        LearningRequest request = new LearningRequest(
                subject,
                grade,
                gradeLevel,
                topic,
                inputText,
                input,
                userQuestion,
                style,
                mode
        );
        return learningService.learnWithPdf(LearningMode.from(mode), request, pdfFile);
    }

    @PostMapping(value = "/learn/explain", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LearningResponse explain(@Valid @RequestBody LearningRequest request) {
        return learningService.learn(LearningMode.EXPLAIN, request);
    }

    @PostMapping(value = "/learn/exercises", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LearningResponse exercises(@Valid @RequestBody LearningRequest request) {
        return learningService.learn(LearningMode.EXERCISES, request);
    }

    @PostMapping(value = "/learn/quiz", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LearningResponse quiz(@Valid @RequestBody LearningRequest request) {
        return learningService.learn(LearningMode.QUIZ, request);
    }

    @PostMapping(value = "/learn/correct", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LearningResponse correct(@Valid @RequestBody LearningRequest request) {
        return learningService.learn(LearningMode.CORRECT, request);
    }

    @PostMapping(value = "/learn/crash-course", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LearningResponse crashCourse(@Valid @RequestBody LearningRequest request) {
        return learningService.learn(LearningMode.CRASH_COURSE, request);
    }

    @PostMapping(value = "/learn/chat", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LearningResponse chat(@Valid @RequestBody LearningRequest request) {
        return learningService.learn(LearningMode.CHAT, request);
    }
}
