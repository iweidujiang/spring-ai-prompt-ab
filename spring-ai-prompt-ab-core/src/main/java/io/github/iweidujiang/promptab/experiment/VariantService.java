package io.github.iweidujiang.promptab.experiment;

import io.github.iweidujiang.promptab.domain.Experiment;
import io.github.iweidujiang.promptab.domain.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * 变体管理服务
 * <p>
 * 负责变体的创建、查询、流量调整和激活/停用。
 * 校验同一实验下所有变体的 trafficPct 之和不超过 100。
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-09
 */
public class VariantService {

    private static final Logger log = LoggerFactory.getLogger(VariantService.class);
    private static final int MAX_TRAFFIC_PCT = 100;

    private final VariantRepository variantRepository;
    private final ExperimentRepository experimentRepository;

    public VariantService(VariantRepository variantRepository,
                          ExperimentRepository experimentRepository) {
        this.variantRepository = variantRepository;
        this.experimentRepository = experimentRepository;
    }

    /**
     * 创建变体
     *
     * @param experimentKey  实验标识
     * @param variantKey     变体标识（实验内唯一）
     * @param promptTemplate Prompt 模板
     * @param trafficPct     流量比例（0-100）
     * @return 创建后的变体对象
     * @throws IllegalArgumentException 如果实验不存在、变体标识重复或流量超限
     */
    public Variant createVariant(String experimentKey, String variantKey,
                                 String promptTemplate, int trafficPct) {
        Experiment experiment = experimentRepository.findByKey(experimentKey)
                .orElseThrow(() -> new IllegalArgumentException("实验不存在: " + experimentKey));

        if (variantRepository.existsByExperimentIdAndVariantKey(experiment.getId(), variantKey)) {
            throw new IllegalArgumentException(
                    String.format("变体标识已存在: %s（实验: %s）", variantKey, experimentKey));
        }

        if (trafficPct < 0 || trafficPct > MAX_TRAFFIC_PCT) {
            throw new IllegalArgumentException("流量比例必须在 0-100 之间: " + trafficPct);
        }

        List<Variant> existingVariants = variantRepository.findByExperimentKey(experimentKey);
        int totalTraffic = existingVariants.stream()
                .mapToInt(Variant::getTrafficPct)
                .sum() + trafficPct;
        if (totalTraffic > MAX_TRAFFIC_PCT) {
            throw new IllegalArgumentException(
                    String.format("流量比例之和不能超过 100（当前总计: %d，新增: %d）",
                            totalTraffic - trafficPct, trafficPct));
        }

        Variant variant = new Variant();
        variant.setExperimentId(experiment.getId());
        variant.setVariantKey(variantKey);
        variant.setPromptTemplate(promptTemplate);
        variant.setTrafficPct(trafficPct);
        variant.setIsActive(false);

        Variant saved = variantRepository.save(variant);
        log.info("创建变体：experimentKey={}, variantKey={}, trafficPct={}",
                experimentKey, variantKey, trafficPct);
        return saved;
    }

    /**
     * 查询实验下所有变体
     *
     * @param experimentKey 实验标识
     * @return 变体列表
     */
    public List<Variant> findByExperimentKey(String experimentKey) {
        return variantRepository.findByExperimentKey(experimentKey);
    }

    /**
     * 根据 ID 查询变体
     *
     * @param id 变体 ID
     * @return 变体对象（可能为空）
     */
    public Optional<Variant> findById(Long id) {
        return variantRepository.findById(id);
    }

    /**
     * 调整变体流量比例
     *
     * @param id         变体 ID
     * @param trafficPct 新的流量比例
     * @return 更新后的变体对象
     * @throws IllegalStateException  如果变体不存在
     * @throws IllegalArgumentException 如果流量超限
     */
    public Variant updateTrafficPct(Long id, int trafficPct) {
        Variant variant = variantRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("变体不存在: " + id));

        if (trafficPct < 0 || trafficPct > MAX_TRAFFIC_PCT) {
            throw new IllegalArgumentException("流量比例必须在 0-100 之间: " + trafficPct);
        }

        String experimentKey = experimentRepository.findById(variant.getExperimentId())
                .orElseThrow(() -> new IllegalStateException("关联实验不存在"))
                .getExperimentKey();

        List<Variant> siblings = variantRepository.findByExperimentKey(experimentKey);
        int totalTraffic = siblings.stream()
                .filter(v -> !v.getId().equals(id))
                .mapToInt(Variant::getTrafficPct)
                .sum() + trafficPct;
        if (totalTraffic > MAX_TRAFFIC_PCT) {
            throw new IllegalArgumentException(
                    String.format("流量比例之和不能超过 100（调整后总计: %d）", totalTraffic));
        }

        variantRepository.updateTrafficPct(id, trafficPct);
        variant.setTrafficPct(trafficPct);
        log.info("调整变体流量：id={}, variantKey={}, trafficPct={}",
                id, variant.getVariantKey(), trafficPct);
        return variant;
    }

    /**
     * 激活/停用变体
     *
     * @param id       变体 ID
     * @param isActive 是否激活
     * @return 更新后的变体对象
     * @throws IllegalStateException 如果变体不存在
     */
    public Variant updateIsActive(Long id, boolean isActive) {
        Variant variant = variantRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("变体不存在: " + id));

        variantRepository.updateIsActive(id, isActive);
        variant.setIsActive(isActive);
        log.info("变体激活状态变更：id={}, variantKey={}, isActive={}",
                id, variant.getVariantKey(), isActive);
        return variant;
    }
}
