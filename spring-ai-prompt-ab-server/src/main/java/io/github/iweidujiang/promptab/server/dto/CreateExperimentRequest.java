package io.github.iweidujiang.promptab.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建实验请求
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-08
 */
public class CreateExperimentRequest {

    @NotBlank(message = "实验标识不能为空")
    @Size(max = 128, message = "实验标识最长 128 字符")
    private String experimentKey;

    @Size(max = 512, message = "描述最长 512 字符")
    private String description;

    public String getExperimentKey() {
        return experimentKey;
    }

    public void setExperimentKey(String experimentKey) {
        this.experimentKey = experimentKey;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
