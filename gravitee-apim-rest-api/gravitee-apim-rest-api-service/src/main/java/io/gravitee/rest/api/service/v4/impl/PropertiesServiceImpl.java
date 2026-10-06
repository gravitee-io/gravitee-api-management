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
package io.gravitee.rest.api.service.v4.impl;

import static io.gravitee.apim.core.utils.EncryptedValueMask.ENCRYPTED_VALUE_MASK;

import io.gravitee.apim.core.api.exception.MaskedApiPropertyValueException;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.rest.api.model.v4.api.properties.PropertyEntity;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.TransactionalService;
import io.gravitee.rest.api.service.v4.PropertiesService;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PropertiesServiceImpl extends TransactionalService implements PropertiesService {

    private final DataEncryptor dataEncryptor;

    public PropertiesServiceImpl(final DataEncryptor dataEncryptor) {
        this.dataEncryptor = dataEncryptor;
    }

    @Override
    public List<PropertyEntity> encryptProperties(List<PropertyEntity> storedProperties, List<PropertyEntity> properties) {
        Map<String, PropertyEntity> storedByKey = indexByKey(storedProperties);
        return properties
            .stream()
            .map(property -> encryptProperty(storedByKey, property))
            .toList();
    }

    private static Map<String, PropertyEntity> indexByKey(List<PropertyEntity> properties) {
        if (properties == null) {
            return Map.of();
        }
        return properties
            .stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(PropertyEntity::getKey, Function.identity(), (first, second) -> first));
    }

    private PropertyEntity encryptProperty(Map<String, PropertyEntity> storedByKey, PropertyEntity property) {
        if (ENCRYPTED_VALUE_MASK.equals(property.getValue())) {
            PropertyEntity stored = Optional.ofNullable(storedByKey.get(property.getKey()))
                .filter(PropertyEntity::isEncrypted)
                .orElseThrow(() -> new MaskedApiPropertyValueException(property.getKey()));
            property.setValue(stored.getValue());
            property.setEncrypted(true);
            return property;
        }
        if (property.isEncryptable() && !property.isEncrypted()) {
            try {
                property.setValue(dataEncryptor.encrypt(property.getValue()));
                property.setEncrypted(true);
            } catch (GeneralSecurityException e) {
                throw new TechnicalManagementException("Unable to encrypt property [" + property.getKey() + "]", e);
            }
        }
        return property;
    }
}
