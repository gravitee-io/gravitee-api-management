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
package io.gravitee.apim.core.api.domain_service.property;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.model.property.EncryptableProperty;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.v4.property.Property;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@DomainService
public class PropertyDomainService {

    private final DataEncryptor dataEncryptor;

    public List<Property> encryptProperties(List<EncryptableProperty> apiProperties) {
        if (apiProperties == null) {
            return new ArrayList<>();
        }
        return apiProperties.stream().map(this::encryptProperty).filter(Objects::nonNull).toList();
    }

    private Property encryptProperty(EncryptableProperty property) {
        if (property == null) {
            return null;
        }
        var asPropertyBuilder = property.toPropertyBuilder();
        if (property.isEncryptable() && !property.isEncrypted()) {
            try {
                asPropertyBuilder.value(dataEncryptor.encrypt(property.getValue())).encrypted(true);
            } catch (GeneralSecurityException e) {
                log.error("Error encrypting property value", e);
            }
        }
        return asPropertyBuilder.build();
    }
}
