package io.github.iweidujiang.promptab.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.iweidujiang.promptab.domain.Experiment;
import io.github.iweidujiang.promptab.experiment.ExperimentService;
import io.github.iweidujiang.promptab.server.dto.CreateExperimentRequest;
import io.github.iweidujiang.promptab.server.dto.UpdateStatusRequest;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ExperimentController 单元测试
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-08
 */
@ExtendWith(MockitoExtension.class)
class ExperimentControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    @Mock
    private ExperimentService experimentService;

    @InjectMocks
    private ExperimentController experimentController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(experimentController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    /**
     * POST /api/experiments —— 创建实验成功
     */
    @Test
    void create_shouldReturn201() throws Exception {
        Experiment saved = createExperiment(1L, "exp-1", "测试实验", "DRAFT");
        when(experimentService.createExperiment("exp-1", "测试实验")).thenReturn(saved);

        CreateExperimentRequest request = new CreateExperimentRequest();
        request.setExperimentKey("exp-1");
        request.setDescription("测试实验");

        mockMvc.perform(post("/api/experiments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.experimentKey").value("exp-1"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    /**
     * POST /api/experiments —— experimentKey 为空时返回 400
     */
    @Test
    void create_shouldReturn400WhenKeyBlank() throws Exception {
        CreateExperimentRequest request = new CreateExperimentRequest();
        request.setExperimentKey("");

        mockMvc.perform(post("/api/experiments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * POST /api/experiments —— 标识重复时返回 400
     */
    @Test
    void create_shouldReturn400WhenDuplicateKey() throws Exception {
        when(experimentService.createExperiment(anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("实验标识已存在: exp-1"));

        CreateExperimentRequest request = new CreateExperimentRequest();
        request.setExperimentKey("exp-1");
        request.setDescription("重复实验");

        mockMvc.perform(post("/api/experiments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("实验标识已存在: exp-1"));
    }

    /**
     * GET /api/experiments/{key} —— 查询存在的实验
     */
    @Test
    void getByKey_shouldReturnExperiment() throws Exception {
        Experiment exp = createExperiment(1L, "exp-1", "描述", "ACTIVE");
        when(experimentService.findByKey("exp-1")).thenReturn(Optional.of(exp));

        mockMvc.perform(get("/api/experiments/exp-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.experimentKey").value("exp-1"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    /**
     * GET /api/experiments/{key} —— 实验不存在时返回 404
     */
    @Test
    void getByKey_shouldReturn404WhenNotFound() throws Exception {
        when(experimentService.findByKey("non-existent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/experiments/non-existent"))
                .andExpect(status().isNotFound());
    }

    /**
     * GET /api/experiments?status=ACTIVE —— 按状态过滤
     */
    @Test
    void list_shouldFilterByStatus() throws Exception {
        Experiment exp1 = createExperiment(1L, "exp-1", "描述1", "ACTIVE");
        Experiment exp2 = createExperiment(2L, "exp-2", "描述2", "ACTIVE");
        when(experimentService.findByStatus("ACTIVE")).thenReturn(List.of(exp1, exp2));

        mockMvc.perform(get("/api/experiments").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].experimentKey").value("exp-1"));
    }

    /**
     * PATCH /api/experiments/{key}/status —— 激活实验
     */
    @Test
    void updateStatus_shouldActivate() throws Exception {
        Experiment exp = createExperiment(1L, "exp-1", "描述", "ACTIVE");
        when(experimentService.activate("exp-1")).thenReturn(exp);

        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("ACTIVE");

        mockMvc.perform(patch("/api/experiments/exp-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    /**
     * PATCH /api/experiments/{key}/status —— 非法状态流转返回 400
     */
    @Test
    void updateStatus_shouldReturn400WhenIllegalTransition() throws Exception {
        when(experimentService.pause("exp-1"))
                .thenThrow(new IllegalStateException("不允许从 DRAFT 转换到 PAUSED"));

        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("PAUSED");

        mockMvc.perform(patch("/api/experiments/exp-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("不允许从 DRAFT 转换到 PAUSED"));
    }

    /**
     * PATCH /api/experiments/{key}/status —— 不支持的状态值返回 400
     */
    @Test
    void updateStatus_shouldReturn400WhenUnsupportedStatus() throws Exception {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("UNKNOWN");

        mockMvc.perform(patch("/api/experiments/exp-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private Experiment createExperiment(Long id, String key, String description, String status) {
        Experiment exp = new Experiment();
        exp.setId(id);
        exp.setExperimentKey(key);
        exp.setDescription(description);
        exp.setStatus(status);
        exp.setCreatedAt(LocalDateTime.of(2026, 10, 8, 10, 0));
        exp.setUpdatedAt(LocalDateTime.of(2026, 10, 8, 10, 0));
        return exp;
    }
}
