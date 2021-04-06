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

import java.util.Objects;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AccessControlEntity {
    private String referenceId;
    private String referenceType;

    public AccessControlEntity() {
    }

    public AccessControlEntity(String referenceId, String referenceType) {
        this.referenceId = referenceId;
        this.referenceType = referenceType;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccessControlEntity accessControl = (AccessControlEntity) o;
        return Objects.equals(referenceId, accessControl.referenceId) &&
            Objects.equals(referenceType, accessControl.referenceType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(referenceType, referenceId);
    }

    @Override
    public String toString() {
        return "AccessControlEntity{" +
            "referenceId='" + referenceId + '\'' +
            ", referenceType='" + referenceType + '\'' +
            '}';
    }

}
