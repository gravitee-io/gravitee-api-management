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
package io.gravitee.repository.management.model;

import java.util.Objects;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Parameter {
    public enum AuditEvent implements Audit.AuditEvent {
        PARAMETER_CREATED, PARAMETER_UPDATED, PARAMETER_DELETED
    }

    private String key;
    private String referenceId;
    private ParameterReferenceType referenceType;
    
    private String value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public ParameterReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(ParameterReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Parameter)) return false;
        Parameter parameter = (Parameter) o;
        return Objects.equals(key, parameter.key) &&
                Objects.equals(referenceId, parameter.referenceId) &&
                referenceType == parameter.referenceType;
    }

    @Override
    public int hashCode() {

        return Objects.hash(key, referenceId, referenceType);
    }

    @Override
    public String toString() {
        return "Parameter{" +
                "key='" + key + '\'' +
                ", referenceId='" + referenceId + '\'' +
                ", referenceType='" + referenceType + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
