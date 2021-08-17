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
package io.gravitee.definition.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.io.Serializable;
import java.util.Objects;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Property implements Serializable {

    @JsonProperty(value = "key", required = true)
    private String key;

    @JsonProperty(value = "value", required = true)
    private String value;

    @JsonProperty("dynamic")
    protected boolean dynamic = false;

    @JsonProperty(value = "encrypted")
    private boolean encrypted = false;

    public Property() {}

    public Property(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public Property(String key, String value, boolean encrypted) {
        this.key = key;
        this.value = value;
        this.encrypted = encrypted;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Property property = (Property) o;
        return (
            dynamic == property.dynamic &&
            encrypted == property.encrypted &&
            Objects.equals(key, property.key) &&
            Objects.equals(value, property.value)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value, dynamic, encrypted);
    }
}
