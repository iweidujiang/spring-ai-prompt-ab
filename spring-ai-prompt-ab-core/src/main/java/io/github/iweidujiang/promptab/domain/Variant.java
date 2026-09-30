package io.github.iweidujiang.promptab.domain;

/**
 * 变体实体
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-09-30
 */
public class Variant {

    private Long id;
    private Long experimentId;
    private String variantKey;
    private String promptTemplate;
    private Integer trafficPct;
    private Boolean isActive;

    public Variant() {
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
