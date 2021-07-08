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
package io.gravitee.repository.mongodb.management.internal.model;

import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "promotions")
public class PromotionMongo extends Auditable {

    @Id
    private String id;

    private String apiDefinition;
    private String apiId;
    private String status;

    private String targetEnvCockpitId;
    private String targetEnvName;

    private String sourceEnvCockpitId;
    private String sourceEnvName;

    private PromotionAuthorMongo author;

    private String targetApiId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApiDefinition() {
        return apiDefinition;
    }

    public void setApiDefinition(String apiDefinition) {
        this.apiDefinition = apiDefinition;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

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

    public PromotionAuthorMongo getAuthor() {
        return author;
    }

    public void setAuthor(PromotionAuthorMongo author) {
        this.author = author;
    }

    public String getTargetApiId() {
        return targetApiId;
    }

    public void setTargetApiId(String targetApiId) {
        this.targetApiId = targetApiId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PromotionMongo that = (PromotionMongo) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(apiDefinition, that.apiDefinition) &&
            Objects.equals(apiId, that.apiId) &&
            Objects.equals(status, that.status) &&
            Objects.equals(targetEnvName, that.targetEnvName) &&
            Objects.equals(targetEnvCockpitId, that.targetEnvCockpitId) &&
            Objects.equals(sourceEnvName, that.sourceEnvName) &&
            Objects.equals(sourceEnvCockpitId, that.sourceEnvCockpitId) &&
            Objects.equals(targetApiId, that.targetApiId)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            apiDefinition,
            apiId,
            status,
            targetEnvName,
            targetEnvCockpitId,
            sourceEnvName,
            sourceEnvCockpitId,
            targetApiId
        );
    }
}
