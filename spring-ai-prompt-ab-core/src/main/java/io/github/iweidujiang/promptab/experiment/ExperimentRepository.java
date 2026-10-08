package io.github.iweidujiang.promptab.experiment;

import io.github.iweidujiang.promptab.domain.Experiment;

import java.util.List;
import java.util.Optional;

/**
 * 实验仓储接口
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-09-30
 */
public interface ExperimentRepository {

    /**
     * 根据实验标识查询实验
     *
     * @param experimentKey 实验标识
     * @return 实验对象（可能为空）
     */
    Optional<Experiment> findByKey(String experimentKey);

    /**
     * 根据状态查询实验列表
     *
     * @param status 实验状态
     * @return 实验列表
     */
    List<Experiment> findByStatus(String status);

    /**
     * 保存实验（新增）
     *
     * @param experiment 实验对象
     * @return 保存后的实验对象（含自增 ID）
     */
    Experiment save(Experiment experiment);

    /**
     * 更新实验状态
     *
     * @param experimentKey 实验标识
     * @param newStatus     新状态
     * @return 是否更新成功
     */
    boolean updateStatus(String experimentKey, String newStatus);
}
