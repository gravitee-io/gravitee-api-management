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
package io.gravitee.rest.api.model.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import javax.validation.constraints.NotNull;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DuplicateApiEntity {

    @NotNull
    @JsonProperty("context_path")
    private String contextPath;

    @JsonProperty("filtered_fields")
    private List<String> filteredFields;

    private String version;

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public List<String> getFilteredFields() {
        return filteredFields;
    }

    public void setFilteredFields(List<String> filteredFields) {
        this.filteredFields = filteredFields;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DuplicateApiEntity that = (DuplicateApiEntity) o;
        return (
            Objects.equals(contextPath, that.contextPath) &&
            Objects.equals(filteredFields, that.filteredFields) &&
            Objects.equals(version, that.version)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(contextPath, filteredFields, version);
    }

    @Override
    public String toString() {
        return (
            "DuplicateApiEntity{" +
            "contextPath='" +
            contextPath +
            '\'' +
            ", filteredFields=" +
            filteredFields +
            ", version='" +
            version +
            '\'' +
            '}'
        );
    }
}
