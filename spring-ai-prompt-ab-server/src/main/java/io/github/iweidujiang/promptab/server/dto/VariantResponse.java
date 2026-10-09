package io.github.iweidujiang.promptab.server.dto;

import io.github.iweidujiang.promptab.domain.Variant;

/**
 * 变体响应 DTO
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-09
 */
public class VariantResponse {

    private Long id;
    private Long experimentId;
    private String variantKey;
    private String promptTemplate;
    private Integer trafficPct;
    private Boolean isActive;

    public static VariantResponse from(Variant variant) {
        VariantResponse response = new VariantResponse();
        response.setId(variant.getId());
        response.setExperimentId(variant.getExperimentId());
        response.setVariantKey(variant.getVariantKey());
        response.setPromptTemplate(variant.getPromptTemplate());
        response.setTrafficPct(variant.getTrafficPct());
        response.setIsActive(variant.getIsActive());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(Long experimentId) {
        this.experimentId = experimentId;
    }

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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
