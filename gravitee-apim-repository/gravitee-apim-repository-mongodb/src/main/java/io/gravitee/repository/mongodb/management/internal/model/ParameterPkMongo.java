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

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ParameterPkMongo implements Serializable {

    private static final long serialVersionUID = 8405585544047569448L;

    private String key;
    private String referenceId;
    private String referenceType;

    public ParameterPkMongo() {}

    public ParameterPkMongo(String key, String referenceId, String referenceType) {
        this.key = key;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParameterPkMongo)) return false;
        ParameterPkMongo that = (ParameterPkMongo) o;
        return Objects.equals(key, that.key) && Objects.equals(referenceId, that.referenceId) && referenceType == that.referenceType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, referenceId, referenceType);
    }

    @Override
    public String toString() {
        return (
            "ParameterPkMongo{" + "key='" + key + '\'' + ", referenceId='" + referenceId + '\'' + ", referenceType=" + referenceType + '}'
        );
    }
}
