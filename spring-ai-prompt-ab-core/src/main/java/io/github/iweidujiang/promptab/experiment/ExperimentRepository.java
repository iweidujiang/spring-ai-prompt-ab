package io.github.iweidujiang.promptab.experiment;

import io.github.iweidujiang.promptab.domain.Experiment;

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
}
