package io.github.iweidujiang.promptab.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.iweidujiang.promptab.domain.Variant;
import io.github.iweidujiang.promptab.experiment.VariantService;
import io.github.iweidujiang.promptab.server.dto.CreateVariantRequest;
import io.github.iweidujiang.promptab.server.dto.UpdateActivateRequest;
import io.github.iweidujiang.promptab.server.dto.UpdateTrafficRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * VariantController 单元测试
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-09
 */
@ExtendWith(MockitoExtension.class)
class VariantControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    @Mock
    private VariantService variantService;

    @InjectMocks
    private VariantController variantController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(variantController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    /**
     * POST /api/experiments/{key}/variants —— 创建变体成功
     */
    @Test
    void create_shouldReturn201() throws Exception {
        Variant saved = createVariant(10L, 1L, "v-a", "模板A", 50, false);
        when(variantService.createVariant("exp-1", "v-a", "模板A", 50)).thenReturn(saved);

        CreateVariantRequest request = new CreateVariantRequest();
        request.setVariantKey("v-a");
        request.setPromptTemplate("模板A");
        request.setTrafficPct(50);

        mockMvc.perform(post("/api/experiments/exp-1/variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.variantKey").value("v-a"))
                .andExpect(jsonPath("$.trafficPct").value(50));
    }

    /**
     * POST /api/experiments/{key}/variants —— variantKey 为空返回 400
     */
    @Test
    void create_shouldReturn400WhenKeyBlank() throws Exception {
        CreateVariantRequest request = new CreateVariantRequest();
        request.setVariantKey("");
        request.setPromptTemplate("模板");
        request.setTrafficPct(50);

        mockMvc.perform(post("/api/experiments/exp-1/variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * POST /api/experiments/{key}/variants —— 实验不存在返回 400
     */
    @Test
    void create_shouldReturn400WhenExperimentNotFound() throws Exception {
        when(variantService.createVariant(anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(new IllegalArgumentException("实验不存在: non-existent"));

        CreateVariantRequest request = new CreateVariantRequest();
        request.setVariantKey("v-a");
        request.setPromptTemplate("模板");
        request.setTrafficPct(50);

        mockMvc.perform(post("/api/experiments/non-existent/variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("实验不存在: non-existent"));
    }

    /**
     * POST /api/experiments/{key}/variants —— 流量超限返回 400
     */
    @Test
    void create_shouldReturn400WhenTrafficExceeded() throws Exception {
        when(variantService.createVariant(anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(new IllegalArgumentException("流量比例之和不能超过 100"));

        CreateVariantRequest request = new CreateVariantRequest();
        request.setVariantKey("v-c");
        request.setPromptTemplate("模板C");
        request.setTrafficPct(80);

        mockMvc.perform(post("/api/experiments/exp-1/variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("流量比例之和不能超过 100"));
    }

    /**
     * GET /api/experiments/{key}/variants —— 查询变体列表
     */
    @Test
    void listByExperiment_shouldReturnList() throws Exception {
        Variant v1 = createVariant(1L, 1L, "v-a", "模板A", 50, true);
        Variant v2 = createVariant(2L, 1L, "v-b", "模板B", 50, true);
        when(variantService.findByExperimentKey("exp-1")).thenReturn(List.of(v1, v2));

        mockMvc.perform(get("/api/experiments/exp-1/variants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].variantKey").value("v-a"))
                .andExpect(jsonPath("$[1].variantKey").value("v-b"));
    }

    /**
     * PATCH /api/variants/{id}/traffic —— 调整流量成功
     */
    @Test
    void updateTraffic_shouldSucceed() throws Exception {
        Variant updated = createVariant(1L, 1L, "v-a", "模板A", 70, true);
        when(variantService.updateTrafficPct(1L, 70)).thenReturn(updated);

        UpdateTrafficRequest request = new UpdateTrafficRequest();
        request.setTrafficPct(70);

        mockMvc.perform(patch("/api/variants/1/traffic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trafficPct").value(70));
    }

    /**
     * PATCH /api/variants/{id}/traffic —— 流量超限返回 400
     */
    @Test
    void updateTraffic_shouldReturn400WhenExceeded() throws Exception {
        when(variantService.updateTrafficPct(1L, 90))
                .thenThrow(new IllegalArgumentException("流量比例之和不能超过 100"));

        UpdateTrafficRequest request = new UpdateTrafficRequest();
        request.setTrafficPct(90);

        mockMvc.perform(patch("/api/variants/1/traffic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * PATCH /api/variants/{id}/activate —— 激活变体
     */
    @Test
    void updateActivate_shouldActivate() throws Exception {
        Variant activated = createVariant(1L, 1L, "v-a", "模板A", 50, true);
        when(variantService.updateIsActive(1L, true)).thenReturn(activated);

        UpdateActivateRequest request = new UpdateActivateRequest();
        request.setIsActive(true);

        mockMvc.perform(patch("/api/variants/1/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(true));
    }

    /**
     * PATCH /api/variants/{id}/activate —— 停用变体
     */
    @Test
    void updateActivate_shouldDeactivate() throws Exception {
        Variant deactivated = createVariant(1L, 1L, "v-a", "模板A", 50, false);
        when(variantService.updateIsActive(1L, false)).thenReturn(deactivated);

        UpdateActivateRequest request = new UpdateActivateRequest();
        request.setIsActive(false);

        mockMvc.perform(patch("/api/variants/1/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    /**
     * PATCH /api/variants/{id}/activate —— 变体不存在返回 400
     */
    @Test
    void updateActivate_shouldReturn400WhenNotFound() throws Exception {
        when(variantService.updateIsActive(999L, true))
                .thenThrow(new IllegalStateException("变体不存在: 999"));

        UpdateActivateRequest request = new UpdateActivateRequest();
        request.setIsActive(true);

        mockMvc.perform(patch("/api/variants/999/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("变体不存在: 999"));
    }

    private Variant createVariant(Long id, Long experimentId, String variantKey,
                                  String promptTemplate, int trafficPct, boolean isActive) {
        Variant v = new Variant();
        v.setId(id);
        v.setExperimentId(experimentId);
        v.setVariantKey(variantKey);
        v.setPromptTemplate(promptTemplate);
        v.setTrafficPct(trafficPct);
        v.setIsActive(isActive);
        return v;
    }
}
