package io.github.iweidujiang.promptab.experiment;

import io.github.iweidujiang.promptab.domain.Variant;

import java.util.List;
import java.util.Optional;

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

    /**
     * 查询指定实验下所有变体（包括未激活的）
     *
     * @param experimentKey 实验标识
     * @return 变体列表
     */
    List<Variant> findByExperimentKey(String experimentKey);

    /**
     * 根据 ID 查询变体
     *
     * @param id 变体 ID
     * @return 变体对象（可能为空）
     */
    Optional<Variant> findById(Long id);

    /**
     * 保存变体（新增）
     *
     * @param variant 变体对象
     * @return 保存后的变体对象（含自增 ID）
     */
    Variant save(Variant variant);

    /**
     * 更新变体流量比例
     *
     * @param id         变体 ID
     * @param trafficPct 新的流量比例
     * @return 是否更新成功
     */
    boolean updateTrafficPct(Long id, int trafficPct);

    /**
     * 更新变体激活状态
     *
     * @param id       变体 ID
     * @param isActive 是否激活
     * @return 是否更新成功
     */
    boolean updateIsActive(Long id, boolean isActive);

    /**
     * 检查同一实验下是否存在指定标识的变体
     *
     * @param experimentId 实验 ID
     * @param variantKey   变体标识
     * @return 是否存在
     */
    boolean existsByExperimentIdAndVariantKey(Long experimentId, String variantKey);
}
