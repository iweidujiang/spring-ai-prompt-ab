package io.github.iweidujiang.promptab.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建变体请求 DTO
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-09
 */
public class CreateVariantRequest {

    @NotBlank(message = "变体标识不能为空")
    private String variantKey;

    @NotBlank(message = "Prompt 模板不能为空")
    private String promptTemplate;

    @NotNull(message = "流量比例不能为空")
    private Integer trafficPct;

    public String getVariantKey() {
        return variantKey;
    }

    public void setVariantKey(String variantKey) {
        this.variantKey = variantKey;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public void setPromptTemplate(String promptTemplate) {
        this.promptTemplate = promptTemplate;
    }

    public Integer getTrafficPct() {
        return trafficPct;
    }

    public void setTrafficPct(Integer trafficPct) {
        this.trafficPct = trafficPct;
    }
}
