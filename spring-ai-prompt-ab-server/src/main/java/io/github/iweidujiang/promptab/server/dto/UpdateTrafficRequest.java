package io.github.iweidujiang.promptab.server.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 调整流量比例请求 DTO
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-09
 */
public class UpdateTrafficRequest {

    @NotNull(message = "流量比例不能为空")
    private Integer trafficPct;

    public Integer getTrafficPct() {
        return trafficPct;
    }

    public void setTrafficPct(Integer trafficPct) {
        this.trafficPct = trafficPct;
    }
}
