package io.github.iweidujiang.promptab.server.controller;

import io.github.iweidujiang.promptab.domain.Variant;
import io.github.iweidujiang.promptab.experiment.VariantService;
import io.github.iweidujiang.promptab.server.dto.CreateVariantRequest;
import io.github.iweidujiang.promptab.server.dto.UpdateActivateRequest;
import io.github.iweidujiang.promptab.server.dto.UpdateTrafficRequest;
import io.github.iweidujiang.promptab.server.dto.VariantResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 变体管理 REST 接口
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-09
 */
@RestController
public class VariantController {

    private final VariantService variantService;

    public VariantController(VariantService variantService) {
        this.variantService = variantService;
    }

    /**
     * 创建变体
     */
    @PostMapping("/api/experiments/{key}/variants")
    public ResponseEntity<VariantResponse> create(@PathVariable String key,
                                                  @Valid @RequestBody CreateVariantRequest request) {
        Variant variant = variantService.createVariant(
                key, request.getVariantKey(),
                request.getPromptTemplate(), request.getTrafficPct());
        return ResponseEntity.status(HttpStatus.CREATED).body(VariantResponse.from(variant));
    }

    /**
     * 查询实验下所有变体
     */
    @GetMapping("/api/experiments/{key}/variants")
    public ResponseEntity<List<VariantResponse>> listByExperiment(@PathVariable String key) {
        List<VariantResponse> variants = variantService.findByExperimentKey(key).stream()
                .map(VariantResponse::from)
                .toList();
        return ResponseEntity.ok(variants);
    }

    /**
     * 调整变体流量比例
     */
    @PatchMapping("/api/variants/{id}/traffic")
    public ResponseEntity<VariantResponse> updateTraffic(@PathVariable Long id,
                                                         @Valid @RequestBody UpdateTrafficRequest request) {
        Variant variant = variantService.updateTrafficPct(id, request.getTrafficPct());
        return ResponseEntity.ok(VariantResponse.from(variant));
    }

    /**
     * 激活/停用变体
     */
    @PatchMapping("/api/variants/{id}/activate")
    public ResponseEntity<VariantResponse> updateActivate(@PathVariable Long id,
                                                          @Valid @RequestBody UpdateActivateRequest request) {
        Variant variant = variantService.updateIsActive(id, request.getIsActive());
        return ResponseEntity.ok(VariantResponse.from(variant));
    }

    /**
     * 全局异常处理：参数非法
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    /**
     * 全局异常处理：变体不存在
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    public record ErrorResponse(String message) {
    }
}
