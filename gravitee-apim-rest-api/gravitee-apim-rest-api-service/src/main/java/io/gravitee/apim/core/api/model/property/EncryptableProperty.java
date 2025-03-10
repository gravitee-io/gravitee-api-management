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
package io.gravitee.apim.core.api.model.property;

import io.gravitee.definition.model.v4.property.Property;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class EncryptableProperty {

    private String key;
    private String value;
    private boolean encrypted;
    private boolean dynamic;
    private boolean encryptable;

    public Property.PropertyBuilder<?, ?> toPropertyBuilder() {
        return Property.builder().key(key).value(value).encrypted(encrypted).dynamic(dynamic);
    }
}
