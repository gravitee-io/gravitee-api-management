/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
public class Entrypoint {

    public enum AuditEvent implements Audit.AuditEvent {
        ENTRYPOINT_CREATED,
        ENTRYPOINT_UPDATED,
        ENTRYPOINT_DELETED,
    }

    private String id;
    private String target;
    private String value;
    private String tags;
    private String referenceId;
    private EntrypointReferenceType referenceType;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public EntrypointReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(EntrypointReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entrypoint)) return false;
        Entrypoint that = (Entrypoint) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return (
            "Entrypoint{" +
            "id='" +
            id +
            '\'' +
            ", target='" +
            target +
            '\'' +
            ", value='" +
            value +
            '\'' +
            ", tags='" +
            tags +
            '\'' +
            ", referenceId='" +
            referenceId +
            '\'' +
            ", referenceType=" +
            referenceType +
            '}'
        );
    }
}
