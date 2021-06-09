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
    private String status;

    private String targetEnvironmentId;
    private String targetInstallationId;
    private String sourceEnvironmentId;
    private String sourceInstallationId;

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

    public String getTargetEnvironmentId() {
        return targetEnvironmentId;
    }

    public void setTargetEnvironmentId(String targetEnvironmentId) {
        this.targetEnvironmentId = targetEnvironmentId;
    }

    public String getTargetInstallationId() {
        return targetInstallationId;
    }

    public void setTargetInstallationId(String targetInstallationId) {
        this.targetInstallationId = targetInstallationId;
    }

    public String getSourceEnvironmentId() {
        return sourceEnvironmentId;
    }

    public void setSourceEnvironmentId(String sourceEnvironmentId) {
        this.sourceEnvironmentId = sourceEnvironmentId;
    }

    public String getSourceInstallationId() {
        return sourceInstallationId;
    }

    public void setSourceInstallationId(String sourceInstallationId) {
        this.sourceInstallationId = sourceInstallationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PromotionMongo that = (PromotionMongo) o;
        return Objects.equals(id, that.id) && Objects.equals(apiDefinition, that.apiDefinition) && Objects.equals(status, that.status) && Objects.equals(targetEnvironmentId, that.targetEnvironmentId) && Objects.equals(targetInstallationId, that.targetInstallationId) && Objects.equals(sourceEnvironmentId, that.sourceEnvironmentId) && Objects.equals(sourceInstallationId, that.sourceInstallationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, apiDefinition, status, targetEnvironmentId, targetInstallationId, sourceEnvironmentId, sourceInstallationId);
    }
}
