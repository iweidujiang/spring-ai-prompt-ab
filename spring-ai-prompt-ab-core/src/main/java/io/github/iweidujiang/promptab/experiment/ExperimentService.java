package io.github.iweidujiang.promptab.experiment;

import io.github.iweidujiang.promptab.domain.Experiment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map;

/**
 * 实验管理服务
 * <p>
 * 负责实验的创建、查询和状态流转。
 * 状态流转规则：
 * <ul>
 *     <li>DRAFT → ACTIVE</li>
 *     <li>ACTIVE → PAUSED / ARCHIVED</li>
 *     <li>PAUSED → ACTIVE / ARCHIVED</li>
 * </ul>
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-08
 */
public class ExperimentService {

    private static final Logger log = LoggerFactory.getLogger(ExperimentService.class);

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_PAUSED = "PAUSED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    /** 合法的状态流转映射 */
    private static final Map<String, Set<String>> VALID_TRANSITIONS = Map.of(
            STATUS_DRAFT, Set.of(STATUS_ACTIVE),
            STATUS_ACTIVE, Set.of(STATUS_PAUSED, STATUS_ARCHIVED),
            STATUS_PAUSED, Set.of(STATUS_ACTIVE, STATUS_ARCHIVED)
    );

    private final ExperimentRepository experimentRepository;

    public ExperimentService(ExperimentRepository experimentRepository) {
        this.experimentRepository = experimentRepository;
    }

    /**
     * 创建实验，状态默认为 DRAFT
     *
     * @param experimentKey 实验标识（唯一）
     * @param description   实验描述
     * @return 创建后的实验对象
     * @throws IllegalArgumentException 如果 experimentKey 已存在
     */
    public Experiment createExperiment(String experimentKey, String description) {
        Optional<Experiment> existing = experimentRepository.findByKey(experimentKey);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("实验标识已存在: " + experimentKey);
        }

        Experiment experiment = new Experiment();
        experiment.setExperimentKey(experimentKey);
        experiment.setDescription(description);
        experiment.setStatus(STATUS_DRAFT);

        Experiment saved = experimentRepository.save(experiment);
        log.info("创建实验：key={}, id={}", experimentKey, saved.getId());
        return saved;
    }

    /**
     * 根据标识查询实验
     *
     * @param experimentKey 实验标识
     * @return 实验对象（可能为空）
     */
    public Optional<Experiment> findByKey(String experimentKey) {
        return experimentRepository.findByKey(experimentKey);
    }

    /**
     * 根据状态查询实验列表
     *
     * @param status 实验状态
     * @return 实验列表
     */
    public List<Experiment> findByStatus(String status) {
        return experimentRepository.findByStatus(status);
    }

    /**
     * 激活实验（DRAFT/PAUSED → ACTIVE）
     *
     * @param experimentKey 实验标识
     * @return 更新后的实验对象
     * @throws IllegalStateException 如果实验不存在或状态流转不合法
     */
    public Experiment activate(String experimentKey) {
        return transitionStatus(experimentKey, STATUS_ACTIVE);
    }

    /**
     * 暂停实验（ACTIVE → PAUSED）
     *
     * @param experimentKey 实验标识
     * @return 更新后的实验对象
     * @throws IllegalStateException 如果实验不存在或状态流转不合法
     */
    public Experiment pause(String experimentKey) {
        return transitionStatus(experimentKey, STATUS_PAUSED);
    }

    /**
     * 归档实验（ACTIVE/PAUSED → ARCHIVED）
     *
     * @param experimentKey 实验标识
     * @return 更新后的实验对象
     * @throws IllegalStateException 如果实验不存在或状态流转不合法
     */
    public Experiment archive(String experimentKey) {
        return transitionStatus(experimentKey, STATUS_ARCHIVED);
    }

    private Experiment transitionStatus(String experimentKey, String targetStatus) {
        Experiment experiment = experimentRepository.findByKey(experimentKey)
                .orElseThrow(() -> new IllegalStateException("实验不存在: " + experimentKey));

        String currentStatus = experiment.getStatus();
        Set<String> allowedTargets = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());

        if (!allowedTargets.contains(targetStatus)) {
            throw new IllegalStateException(
                    String.format("不允许从 %s 转换到 %s（实验: %s）", currentStatus, targetStatus, experimentKey));
        }

        experimentRepository.updateStatus(experimentKey, targetStatus);
        experiment.setStatus(targetStatus);
        log.info("实验状态变更：key={}, {} → {}", experimentKey, currentStatus, targetStatus);
        return experiment;
    }
}
