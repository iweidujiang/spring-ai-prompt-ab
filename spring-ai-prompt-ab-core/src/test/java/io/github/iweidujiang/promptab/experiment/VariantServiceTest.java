package io.github.iweidujiang.promptab.experiment;

import io.github.iweidujiang.promptab.domain.Experiment;
import io.github.iweidujiang.promptab.domain.Variant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * VariantService 单元测试
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-09
 */
@ExtendWith(MockitoExtension.class)
class VariantServiceTest {

    @Mock
    private VariantRepository variantRepository;

    @Mock
    private ExperimentRepository experimentRepository;

    private VariantService variantService;

    @BeforeEach
    void setUp() {
        variantService = new VariantService(variantRepository, experimentRepository);
    }

    /**
     * 创建变体成功
     */
    @Test
    void createVariant_shouldSucceed() {
        Experiment exp = createExperiment(1L, "exp-1");
        when(experimentRepository.findByKey("exp-1")).thenReturn(Optional.of(exp));
        when(variantRepository.existsByExperimentIdAndVariantKey(1L, "v-a")).thenReturn(false);
        when(variantRepository.findByExperimentKey("exp-1")).thenReturn(List.of());
        when(variantRepository.save(any(Variant.class))).thenAnswer(inv -> {
            Variant v = inv.getArgument(0);
            v.setId(10L);
            return v;
        });

        Variant result = variantService.createVariant("exp-1", "v-a", "模板A", 50);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getVariantKey()).isEqualTo("v-a");
        assertThat(result.getTrafficPct()).isEqualTo(50);
        assertThat(result.getIsActive()).isFalse();
        verify(variantRepository).save(any(Variant.class));
    }

    /**
     * 创建变体时实验不存在
     */
    @Test
    void createVariant_shouldThrowWhenExperimentNotFound() {
        when(experimentRepository.findByKey("non-existent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> variantService.createVariant("non-existent", "v-a", "模板", 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("实验不存在");
    }

    /**
     * 创建变体时标识重复
     */
    @Test
    void createVariant_shouldThrowWhenDuplicateKey() {
        Experiment exp = createExperiment(1L, "exp-1");
        when(experimentRepository.findByKey("exp-1")).thenReturn(Optional.of(exp));
        when(variantRepository.existsByExperimentIdAndVariantKey(1L, "v-a")).thenReturn(true);

        assertThatThrownBy(() -> variantService.createVariant("exp-1", "v-a", "模板", 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("变体标识已存在");
    }

    /**
     * 创建变体时流量比例超限
     */
    @Test
    void createVariant_shouldThrowWhenTrafficExceeds100() {
        Experiment exp = createExperiment(1L, "exp-1");
        when(experimentRepository.findByKey("exp-1")).thenReturn(Optional.of(exp));
        when(variantRepository.existsByExperimentIdAndVariantKey(1L, "v-b")).thenReturn(false);

        Variant existing = createVariant(10L, 1L, "v-a", 60);
        when(variantRepository.findByExperimentKey("exp-1")).thenReturn(List.of(existing));

        assertThatThrownBy(() -> variantService.createVariant("exp-1", "v-b", "模板B", 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("流量比例之和不能超过 100");
    }

    /**
     * 创建变体时流量比例为负数
     */
    @Test
    void createVariant_shouldThrowWhenTrafficNegative() {
        Experiment exp = createExperiment(1L, "exp-1");
        when(experimentRepository.findByKey("exp-1")).thenReturn(Optional.of(exp));
        when(variantRepository.existsByExperimentIdAndVariantKey(1L, "v-a")).thenReturn(false);

        assertThatThrownBy(() -> variantService.createVariant("exp-1", "v-a", "模板", -10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("流量比例必须在 0-100 之间");
    }

    /**
     * 查询实验下所有变体
     */
    @Test
    void findByExperimentKey_shouldReturnList() {
        Variant v1 = createVariant(1L, 1L, "v-a", 50);
        Variant v2 = createVariant(2L, 1L, "v-b", 50);
        when(variantRepository.findByExperimentKey("exp-1")).thenReturn(List.of(v1, v2));

        List<Variant> result = variantService.findByExperimentKey("exp-1");

        assertThat(result).hasSize(2);
    }

    /**
     * 调整流量比例成功
     */
    @Test
    void updateTrafficPct_shouldSucceed() {
        Variant variant = createVariant(1L, 1L, "v-a", 30);
        when(variantRepository.findById(1L)).thenReturn(Optional.of(variant));

        Experiment exp = createExperiment(1L, "exp-1");
        when(experimentRepository.findById(1L)).thenReturn(Optional.of(exp));
        when(variantRepository.findByExperimentKey("exp-1")).thenReturn(List.of(variant));
        when(variantRepository.updateTrafficPct(1L, 60)).thenReturn(true);

        Variant result = variantService.updateTrafficPct(1L, 60);

        assertThat(result.getTrafficPct()).isEqualTo(60);
    }

    /**
     * 调整流量比例时变体不存在
     */
    @Test
    void updateTrafficPct_shouldThrowWhenNotFound() {
        when(variantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> variantService.updateTrafficPct(999L, 50))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("变体不存在");
    }

    /**
     * 调整流量比例后总和超限
     */
    @Test
    void updateTrafficPct_shouldThrowWhenTotalExceeds100() {
        Variant v1 = createVariant(1L, 1L, "v-a", 60);
        Variant v2 = createVariant(2L, 1L, "v-b", 30);
        when(variantRepository.findById(2L)).thenReturn(Optional.of(v2));

        Experiment exp = createExperiment(1L, "exp-1");
        when(experimentRepository.findById(1L)).thenReturn(Optional.of(exp));
        when(variantRepository.findByExperimentKey("exp-1")).thenReturn(List.of(v1, v2));

        assertThatThrownBy(() -> variantService.updateTrafficPct(2L, 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("流量比例之和不能超过 100");
    }

    /**
     * 激活变体成功
     */
    @Test
    void updateIsActive_shouldActivate() {
        Variant variant = createVariant(1L, 1L, "v-a", 50);
        variant.setIsActive(false);
        when(variantRepository.findById(1L)).thenReturn(Optional.of(variant));
        when(variantRepository.updateIsActive(1L, true)).thenReturn(true);

        Variant result = variantService.updateIsActive(1L, true);

        assertThat(result.getIsActive()).isTrue();
    }

    /**
     * 停用变体成功
     */
    @Test
    void updateIsActive_shouldDeactivate() {
        Variant variant = createVariant(1L, 1L, "v-a", 50);
        variant.setIsActive(true);
        when(variantRepository.findById(1L)).thenReturn(Optional.of(variant));
        when(variantRepository.updateIsActive(1L, false)).thenReturn(true);

        Variant result = variantService.updateIsActive(1L, false);

        assertThat(result.getIsActive()).isFalse();
    }

    /**
     * 激活/停用时变体不存在
     */
    @Test
    void updateIsActive_shouldThrowWhenNotFound() {
        when(variantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> variantService.updateIsActive(999L, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("变体不存在");
    }

    private Experiment createExperiment(Long id, String key) {
        Experiment exp = new Experiment();
        exp.setId(id);
        exp.setExperimentKey(key);
        exp.setStatus("DRAFT");
        return exp;
    }

    private Variant createVariant(Long id, Long experimentId, String variantKey, int trafficPct) {
        Variant v = new Variant();
        v.setId(id);
        v.setExperimentId(experimentId);
        v.setVariantKey(variantKey);
        v.setPromptTemplate("模板");
        v.setTrafficPct(trafficPct);
        v.setIsActive(false);
        return v;
    }
}
