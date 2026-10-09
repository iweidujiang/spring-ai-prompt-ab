package io.github.iweidujiang.promptab.server.controller;

import io.github.iweidujiang.promptab.domain.Experiment;
import io.github.iweidujiang.promptab.experiment.ExperimentService;
import io.github.iweidujiang.promptab.server.dto.CreateExperimentRequest;
import io.github.iweidujiang.promptab.server.dto.ExperimentResponse;
import io.github.iweidujiang.promptab.server.dto.UpdateStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 实验管理 REST 接口
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-08
 */
@RestController
@RequestMapping("/api/experiments")
public class ExperimentController {

    private final ExperimentService experimentService;

    public ExperimentController(ExperimentService experimentService) {
        this.experimentService = experimentService;
    }

    /**
     * 创建实验
     */
    @PostMapping
    public ResponseEntity<ExperimentResponse> create(@Valid @RequestBody CreateExperimentRequest request) {
        Experiment experiment = experimentService.createExperiment(
                request.getExperimentKey(), request.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(ExperimentResponse.from(experiment));
    }

    /**
     * 根据标识查询实验
     */
    @GetMapping("/{key}")
    public ResponseEntity<ExperimentResponse> getByKey(@PathVariable String key) {
        return experimentService.findByKey(key)
                .map(exp -> ResponseEntity.ok(ExperimentResponse.from(exp)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 列表查询（支持状态过滤）
     */
    @GetMapping
    public ResponseEntity<List<ExperimentResponse>> list(@RequestParam(required = false) String status) {
        List<Experiment> experiments;
        if (status != null && !status.isBlank()) {
            experiments = experimentService.findByStatus(status);
        } else {
            // 查询所有状态的实验
            experiments = experimentService.findByStatus("DRAFT");
            experiments = new java.util.ArrayList<>(experiments);
            experiments.addAll(experimentService.findByStatus("ACTIVE"));
            experiments.addAll(experimentService.findByStatus("PAUSED"));
            experiments.addAll(experimentService.findByStatus("ARCHIVED"));
        }
        List<ExperimentResponse> responses = experiments.stream()
                .map(ExperimentResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * 状态变更
     */
    @PatchMapping("/{key}/status")
    public ResponseEntity<ExperimentResponse> updateStatus(
            @PathVariable String key,
            @Valid @RequestBody UpdateStatusRequest request) {
        Experiment experiment = switch (request.getStatus().toUpperCase()) {
            case "ACTIVE" -> experimentService.activate(key);
            case "PAUSED" -> experimentService.pause(key);
            case "ARCHIVED" -> experimentService.archive(key);
            default -> throw new IllegalArgumentException("不支持的目标状态: " + request.getStatus());
        };
        return ResponseEntity.ok(ExperimentResponse.from(experiment));
    }

    /**
     * 全局异常处理：实验标识重复
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    /**
     * 全局异常处理：状态流转不合法 / 实验不存在
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    /**
     * 错误响应
     */
    public record ErrorResponse(String message) {
    }
}
