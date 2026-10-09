package io.github.iweidujiang.promptab.server.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 激活/停用变体请求 DTO
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-09
 */
public class UpdateActivateRequest {

    @NotNull(message = "激活状态不能为空")
    private Boolean isActive;

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
