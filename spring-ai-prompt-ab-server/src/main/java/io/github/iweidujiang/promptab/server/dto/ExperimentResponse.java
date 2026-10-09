package io.github.iweidujiang.promptab.server.dto;

import io.github.iweidujiang.promptab.domain.Experiment;

import java.time.LocalDateTime;

/**
 * 实验响应 DTO
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-08
 */
public class ExperimentResponse {

    private Long id;
    private String experimentKey;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ExperimentResponse from(Experiment experiment) {
        ExperimentResponse response = new ExperimentResponse();
        response.setId(experiment.getId());
        response.setExperimentKey(experiment.getExperimentKey());
        response.setDescription(experiment.getDescription());
        response.setStatus(experiment.getStatus());
        response.setCreatedAt(experiment.getCreatedAt());
        response.setUpdatedAt(experiment.getUpdatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
