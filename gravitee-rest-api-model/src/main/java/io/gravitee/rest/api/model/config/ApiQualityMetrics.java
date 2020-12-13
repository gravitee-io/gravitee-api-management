/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.gravitee.rest.api.model.annotations.ParameterKey;
import io.gravitee.rest.api.model.parameters.Key;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiQualityMetrics {

    @ParameterKey(Key.API_QUALITY_METRICS_ENABLED)
    private Boolean enabled;

    @ParameterKey(Key.API_QUALITY_METRICS_FUNCTIONAL_DOCUMENTATION_WEIGHT)
    private Integer functionalDocumentationWeight;

    @ParameterKey(Key.API_QUALITY_METRICS_TECHNICAL_DOCUMENTATION_WEIGHT)
    private Integer technicalDocumentationWeight;

    @ParameterKey(Key.API_QUALITY_METRICS_HEALTHCHECK_WEIGHT)
    private Integer HealthcheckWeight;

    @ParameterKey(Key.API_QUALITY_METRICS_DESCRIPTION_WEIGHT)
    private Integer descriptionWeight;

    @ParameterKey(Key.API_QUALITY_METRICS_DESCRIPTION_MIN_LENGTH)
    private Integer descriptionMinLength;

    @ParameterKey(Key.API_QUALITY_METRICS_LOGO_WEIGHT)
    private Integer logoWeight;

    @ParameterKey(Key.API_QUALITY_METRICS_CATEGORIES_WEIGHT)
    private Integer categoriesWeight;

    @ParameterKey(Key.API_QUALITY_METRICS_LABELS_WEIGHT)
    private Integer labelsWeight;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getFunctionalDocumentationWeight() {
        return functionalDocumentationWeight;
    }

    public void setFunctionalDocumentationWeight(Integer functionalDocumentationWeight) {
        this.functionalDocumentationWeight = functionalDocumentationWeight;
    }

    public Integer getTechnicalDocumentationWeight() {
        return technicalDocumentationWeight;
    }

    public void setTechnicalDocumentationWeight(Integer technicalDocumentationWeight) {
        this.technicalDocumentationWeight = technicalDocumentationWeight;
    }

    public Integer getHealthcheckWeight() {
        return HealthcheckWeight;
    }

    public void setHealthcheckWeight(Integer healthcheckWeight) {
        HealthcheckWeight = healthcheckWeight;
    }

    public Integer getDescriptionWeight() {
        return descriptionWeight;
    }

    public void setDescriptionWeight(Integer descriptionWeight) {
        this.descriptionWeight = descriptionWeight;
    }

    public Integer getDescriptionMinLength() {
        return descriptionMinLength;
    }

    public void setDescriptionMinLength(Integer descriptionMinLength) {
        this.descriptionMinLength = descriptionMinLength;
    }

    public Integer getLogoWeight() {
        return logoWeight;
    }

    public void setLogoWeight(Integer logoWeight) {
        this.logoWeight = logoWeight;
    }

    public Integer getCategoriesWeight() {
        return categoriesWeight;
    }

    public void setCategoriesWeight(Integer categoriesWeight) {
        this.categoriesWeight = categoriesWeight;
    }

    public Integer getLabelsWeight() {
        return labelsWeight;
    }

    public void setLabelsWeight(Integer labelsWeight) {
        this.labelsWeight = labelsWeight;
    }
}
