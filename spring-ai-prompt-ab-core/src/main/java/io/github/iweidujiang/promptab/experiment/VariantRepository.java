package io.github.iweidujiang.promptab.experiment;

import io.github.iweidujiang.promptab.domain.Variant;

import java.util.List;

/**
 * 变体仓储接口
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-09-30
 */
public interface VariantRepository {

    /**
     * 查询指定实验下所有激活的变体
     *
     * @param experimentKey 实验标识
     * @return 激活状态的变体列表
     */
    List<Variant> findActiveByExperimentKey(String experimentKey);
}
