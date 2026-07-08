package com.kickdenstoff.backend.controller;

import java.util.Map;

import com.kickdenstoff.backend.config.LmStudioProperties;
import com.kickdenstoff.backend.dto.LearningMode;
import com.kickdenstoff.backend.dto.LearningRequest;
import com.kickdenstoff.backend.dto.LearningResponse;
import com.kickdenstoff.backend.service.LearningService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/learn")
    public LearningResponse learn(@Valid @RequestBody LearningRequest request) {
        return learningService.learn(LearningMode.from(request.mode()), request);
    }

    @PostMapping("/learn/explain")
    public LearningResponse explain(@Valid @RequestBody LearningRequest request) {
        return learningService.learn(LearningMode.EXPLAIN, request);
    }

    @PostMapping("/learn/exercises")
    public LearningResponse exercises(@Valid @RequestBody LearningRequest request) {
        return learningService.learn(LearningMode.EXERCISES, request);
    }

    @PostMapping("/learn/quiz")
    public LearningResponse quiz(@Valid @RequestBody LearningRequest request) {
        return learningService.learn(LearningMode.QUIZ, request);
    }

    @PostMapping("/learn/correct")
    public LearningResponse correct(@Valid @RequestBody LearningRequest request) {
        return learningService.learn(LearningMode.CORRECT, request);
    }

    @PostMapping("/learn/crash-course")
    public LearningResponse crashCourse(@Valid @RequestBody LearningRequest request) {
        return learningService.learn(LearningMode.CRASH_COURSE, request);
    }

    @PostMapping("/learn/chat")
    public LearningResponse chat(@Valid @RequestBody LearningRequest request) {
        return learningService.learn(LearningMode.CHAT, request);
    }
}
