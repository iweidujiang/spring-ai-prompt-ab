package io.github.iweidujiang.promptab.evaluator;

import io.github.iweidujiang.promptab.domain.MetricEvent;

/**
 * 指标事件仓储接口
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-09
 */
public interface MetricEventRepository {

    /**
     * 保存指标事件
     *
     * @param event 指标事件
     */
    void save(MetricEvent event);

    /**
     * 按实验标识 + 变体标识聚合查询统计信息
     *
     * @param experimentKey 实验标识
     * @param variantKey    变体标识
     * @return 聚合统计（平均分、P99 延迟、样本数）
     */
    MetricStats findStats(String experimentKey, String variantKey);
}
