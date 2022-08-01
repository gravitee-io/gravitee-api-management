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
package io.gravitee.rest.api.model.v4.api.properties;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.v4.property.Property;
import lombok.*;

/**
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class PropertyEntity extends Property {

    @JsonProperty(value = "encryptable")
    private boolean encryptable = false;

    public PropertyEntity(String key, String value) {
        super(key, value);
    }

    public PropertyEntity(String key, String value, boolean encryptable, boolean encrypted) {
        super(key, value, encrypted);
        this.encryptable = encryptable;
    }

    public PropertyEntity(Property property) {
        super(property.getKey(), property.getValue(), property.isEncrypted(), property.isDynamic());
    }

    public boolean isEncryptable() {
        return encryptable;
    }
}
