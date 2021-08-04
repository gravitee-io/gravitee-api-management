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
package io.gravitee.rest.api.model.promotion;

import java.util.Date;

/**
 * @author Yann Tavernier (yann.tavernier at graviteesource.com)
 * @author Gaetan Maisse (gaetan.maisse at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PromotionEntity {

    private String id;
    private String apiDefinition;
    private String apiId;

    private PromotionEntityStatus status;

    private String targetEnvCockpitId;
    private String targetEnvName;

    private String sourceEnvCockpitId;
    private String sourceEnvName;

    private Date createdAt;
    private Date updatedAt;

    private PromotionEntityAuthor author;

    private String targetApiId;

    public String getTargetEnvCockpitId() {
        return targetEnvCockpitId;
    }

    public void setTargetEnvCockpitId(String targetEnvCockpitId) {
        this.targetEnvCockpitId = targetEnvCockpitId;
    }

    public String getSourceEnvCockpitId() {
        return sourceEnvCockpitId;
    }

    public void setSourceEnvCockpitId(String sourceEnvCockpitId) {
        this.sourceEnvCockpitId = sourceEnvCockpitId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setStatus(PromotionEntityStatus status) {
        this.status = status;
    }

    public PromotionEntityStatus getStatus() {
        return status;
    }

    public void setApiDefinition(String apiDefinition) {
        this.apiDefinition = apiDefinition;
    }

    public String getApiDefinition() {
        return apiDefinition;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public String getTargetEnvName() {
        return targetEnvName;
    }

    public void setTargetEnvName(String targetEnvName) {
        this.targetEnvName = targetEnvName;
    }

    public String getSourceEnvName() {
        return sourceEnvName;
    }

    public void setSourceEnvName(String sourceEnvName) {
        this.sourceEnvName = sourceEnvName;
    }

    public PromotionEntityAuthor getAuthor() {
        return author;
    }

    public void setAuthor(PromotionEntityAuthor author) {
        this.author = author;
    }

    public String getTargetApiId() {
        return targetApiId;
    }

    public void setTargetApiId(String targetApiId) {
        this.targetApiId = targetApiId;
    }
}
