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
package io.gravitee.rest.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UpdateDashboardEntity {

    private String id;

    @NotNull
    @JsonProperty("reference_type")
    private DashboardReferenceType referenceType;

    @NotNull
    @Size(max = 64)
    @JsonProperty("reference_id")
    private String referenceId;

    @NotNull
    @Size(max = 64)
    private String name;

    @JsonProperty("query_filter")
    private String queryFilter;

    @NotNull
    private int order;

    private boolean enabled;
    private String definition;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DashboardReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(DashboardReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQueryFilter() {
        return queryFilter;
    }

    public void setQueryFilter(String queryFilter) {
        this.queryFilter = queryFilter;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpdateDashboardEntity that = (UpdateDashboardEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return (
            "UpdateDashboardEntity{" +
            "id='" +
            id +
            '\'' +
            ", referenceType=" +
            referenceType +
            ", referenceId='" +
            referenceId +
            '\'' +
            ", name='" +
            name +
            '\'' +
            ", queryFilter='" +
            queryFilter +
            '\'' +
            ", order=" +
            order +
            ", enabled=" +
            enabled +
            ", definition='" +
            definition +
            '\'' +
            '}'
        );
    }
}
