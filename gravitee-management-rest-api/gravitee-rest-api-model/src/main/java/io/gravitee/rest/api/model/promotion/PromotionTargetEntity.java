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

import io.gravitee.rest.api.model.EnvironmentEntity;
import java.util.List;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PromotionTargetEntity {

    private String id;

    private List<String> hrids;

    @NotNull
    @Size(min = 1)
    private String name;

    private String description;

    @NotNull
    private String organizationId;

    @NotNull
    private String installationId;

    public PromotionTargetEntity() {}

    public PromotionTargetEntity(EnvironmentEntity environmentEntity, String organizationId, String environmentId, String installationId) {
        this.organizationId = organizationId;
        this.id = environmentId;
        this.installationId = installationId;
        this.description = environmentEntity.getDescription();
        this.hrids = environmentEntity.getHrids();
        this.name = environmentEntity.getName();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getInstallationId() {
        return installationId;
    }

    public void setInstallationId(String installationId) {
        this.installationId = installationId;
    }

    public List<String> getHrids() {
        return hrids;
    }

    public void setHrids(List<String> hrids) {
        this.hrids = hrids;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PromotionTargetEntity that = (PromotionTargetEntity) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(hrids, that.hrids) &&
            Objects.equals(name, that.name) &&
            Objects.equals(description, that.description) &&
            Objects.equals(organizationId, that.organizationId) &&
            Objects.equals(installationId, that.installationId)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, hrids, name, description, organizationId, installationId);
    }

    @Override
    public String toString() {
        return (
            "EnvironmentEntity{" +
            "id='" +
            id +
            '\'' +
            ", hrids=" +
            hrids +
            ", name='" +
            name +
            '\'' +
            ", description='" +
            description +
            '\'' +
            ", organizationId='" +
            organizationId +
            '\'' +
            ", installationId=" +
            installationId +
            '}'
        );
    }
}
