package io.github.iweidujiang.promptab.server.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 更新实验状态请求
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-08
 */
public class UpdateStatusRequest {

    @NotBlank(message = "目标状态不能为空")
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
