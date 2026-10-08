package io.github.iweidujiang.promptab.experiment;

import io.github.iweidujiang.promptab.domain.Experiment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ExperimentService 单元测试
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-08
 */
class ExperimentServiceTest {

    private ExperimentRepository experimentRepository;
    private ExperimentService experimentService;

    @BeforeEach
    void setUp() {
        experimentRepository = mock(ExperimentRepository.class);
        experimentService = new ExperimentService(experimentRepository);
    }

    /**
     * 创建实验：状态默认为 DRAFT，返回含 ID 的实验对象
     */
    @Test
    void createExperiment_shouldSetStatusToDraftAndReturnSaved() {
        when(experimentRepository.findByKey("exp-1")).thenReturn(Optional.empty());
        when(experimentRepository.save(any(Experiment.class))).thenAnswer(invocation -> {
            Experiment exp = invocation.getArgument(0);
            exp.setId(1L);
            return exp;
        });

        Experiment result = experimentService.createExperiment("exp-1", "测试实验");

        assertEquals("exp-1", result.getExperimentKey());
        assertEquals("测试实验", result.getDescription());
        assertEquals("DRAFT", result.getStatus());
        assertEquals(1L, result.getId());
        verify(experimentRepository).save(argThat(exp -> "DRAFT".equals(exp.getStatus())));
    }

    /**
     * 创建实验：标识已存在时抛出异常
     */
    @Test
    void createExperiment_shouldThrowWhenKeyAlreadyExists() {
        Experiment existing = new Experiment();
        existing.setExperimentKey("exp-1");
        when(experimentRepository.findByKey("exp-1")).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class,
                () -> experimentService.createExperiment("exp-1", "重复实验"));
    }

    /**
     * 按标识查询实验
     */
    @Test
    void findByKey_shouldDelegateToRepository() {
        Experiment exp = new Experiment();
        exp.setExperimentKey("exp-1");
        when(experimentRepository.findByKey("exp-1")).thenReturn(Optional.of(exp));

        Optional<Experiment> result = experimentService.findByKey("exp-1");

        assertTrue(result.isPresent());
        assertEquals("exp-1", result.get().getExperimentKey());
    }

    /**
     * 按状态查询实验列表
     */
    @Test
    void findByStatus_shouldReturnList() {
        Experiment exp1 = new Experiment();
        exp1.setStatus("ACTIVE");
        Experiment exp2 = new Experiment();
        exp2.setStatus("ACTIVE");
        when(experimentRepository.findByStatus("ACTIVE")).thenReturn(List.of(exp1, exp2));

        List<Experiment> result = experimentService.findByStatus("ACTIVE");

        assertEquals(2, result.size());
    }

    /**
     * 激活实验：DRAFT → ACTIVE
     */
    @Test
    void activate_shouldTransitionFromDraftToActive() {
        Experiment exp = createExperimentWithStatus("exp-1", "DRAFT");
        when(experimentRepository.findByKey("exp-1")).thenReturn(Optional.of(exp));
        when(experimentRepository.updateStatus("exp-1", "ACTIVE")).thenReturn(true);

        Experiment result = experimentService.activate("exp-1");

        assertEquals("ACTIVE", result.getStatus());
        verify(experimentRepository).updateStatus("exp-1", "ACTIVE");
    }

    /**
     * 激活实验：PAUSED → ACTIVE
     */
    @Test
    void activate_shouldTransitionFromPausedToActive() {
        Experiment exp = createExperimentWithStatus("exp-1", "PAUSED");
        when(experimentRepository.findByKey("exp-1")).thenReturn(Optional.of(exp));
        when(experimentRepository.updateStatus("exp-1", "ACTIVE")).thenReturn(true);

        Experiment result = experimentService.activate("exp-1");

        assertEquals("ACTIVE", result.getStatus());
    }

    /**
     * 暂停实验：ACTIVE → PAUSED
     */
    @Test
    void pause_shouldTransitionFromActiveToPaused() {
        Experiment exp = createExperimentWithStatus("exp-1", "ACTIVE");
        when(experimentRepository.findByKey("exp-1")).thenReturn(Optional.of(exp));
        when(experimentRepository.updateStatus("exp-1", "PAUSED")).thenReturn(true);

        Experiment result = experimentService.pause("exp-1");

        assertEquals("PAUSED", result.getStatus());
    }

    /**
     * 归档实验：ACTIVE → ARCHIVED
     */
    @Test
    void archive_shouldTransitionFromActiveToArchived() {
        Experiment exp = createExperimentWithStatus("exp-1", "ACTIVE");
        when(experimentRepository.findByKey("exp-1")).thenReturn(Optional.of(exp));
        when(experimentRepository.updateStatus("exp-1", "ARCHIVED")).thenReturn(true);

        Experiment result = experimentService.archive("exp-1");

        assertEquals("ARCHIVED", result.getStatus());
    }

    /**
     * 归档实验：PAUSED → ARCHIVED
     */
    @Test
    void archive_shouldTransitionFromPausedToArchived() {
        Experiment exp = createExperimentWithStatus("exp-1", "PAUSED");
        when(experimentRepository.findByKey("exp-1")).thenReturn(Optional.of(exp));
        when(experimentRepository.updateStatus("exp-1", "ARCHIVED")).thenReturn(true);

        Experiment result = experimentService.archive("exp-1");

        assertEquals("ARCHIVED", result.getStatus());
    }

    /**
     * 非法状态流转：DRAFT → PAUSED 应抛出异常
     */
    @Test
    void pause_shouldThrowWhenTransitionFromDraft() {
        Experiment exp = createExperimentWithStatus("exp-1", "DRAFT");
        when(experimentRepository.findByKey("exp-1")).thenReturn(Optional.of(exp));

        assertThrows(IllegalStateException.class, () -> experimentService.pause("exp-1"));
    }

    /**
     * 非法状态流转：ARCHIVED → ACTIVE 应抛出异常
     */
    @Test
    void activate_shouldThrowWhenTransitionFromArchived() {
        Experiment exp = createExperimentWithStatus("exp-1", "ARCHIVED");
        when(experimentRepository.findByKey("exp-1")).thenReturn(Optional.of(exp));

        assertThrows(IllegalStateException.class, () -> experimentService.activate("exp-1"));
    }

    /**
     * 实验不存在时抛出异常
     */
    @Test
    void activate_shouldThrowWhenExperimentNotFound() {
        when(experimentRepository.findByKey("non-existent")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> experimentService.activate("non-existent"));
    }

    private Experiment createExperimentWithStatus(String key, String status) {
        Experiment exp = new Experiment();
        exp.setId(1L);
        exp.setExperimentKey(key);
        exp.setStatus(status);
        return exp;
    }
}
