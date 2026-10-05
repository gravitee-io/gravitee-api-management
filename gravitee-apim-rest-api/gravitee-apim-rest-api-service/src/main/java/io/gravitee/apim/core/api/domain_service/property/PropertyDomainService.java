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
import io.gravitee.apim.core.api.exception.ApiPropertyNotCiphertextException;
import io.gravitee.apim.core.api.model.property.EncryptableProperty;
import io.gravitee.apim.core.api.model.property.PropertyClassificationValidator;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.ApiDefinition;
import io.gravitee.definition.model.v4.AbstractApi;
import io.gravitee.definition.model.v4.property.Property;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.CustomLog;

@CustomLog
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
                throw new TechnicalDomainException("Unable to encrypt property [" + property.getKey() + "]", e);
            }
        }
        return asPropertyBuilder.build();
    }

    public void validateClassification(ApiDefinition storedDefinition, List<EncryptableProperty> incomingProperties) {
        if (storedDefinition instanceof AbstractApi definition) {
            validateClassification(definition.getProperties(), incomingProperties);
        }
    }

    public void validateClassification(List<Property> storedProperties, List<EncryptableProperty> incomingProperties) {
        PropertyClassificationValidator.rejectEncryptedToPlain(storedProperties, incomingProperties);
        rejectChangedValuesThatAreNotCiphertext(storedProperties, incomingProperties);
    }

    private void rejectChangedValuesThatAreNotCiphertext(List<Property> storedProperties, List<EncryptableProperty> incomingProperties) {
        if (storedProperties == null || incomingProperties == null) {
            return;
        }
        Map<String, String> storedCiphertextByKey = storedProperties
            .stream()
            .filter(Objects::nonNull)
            .filter(Property::isEncrypted)
            .filter(property -> property.getValue() != null)
            .collect(Collectors.toMap(Property::getKey, Property::getValue, (first, second) -> first));
        incomingProperties
            .stream()
            .filter(Objects::nonNull)
            .filter(EncryptableProperty::isEncrypted)
            .filter(incoming -> storedCiphertextByKey.containsKey(incoming.getKey()))
            .filter(incoming -> !storedCiphertextByKey.get(incoming.getKey()).equals(incoming.getValue()))
            .filter(incoming -> !decrypts(incoming.getValue()))
            .findFirst()
            .ifPresent(incoming -> {
                throw new ApiPropertyNotCiphertextException(incoming.getKey());
            });
    }

    private boolean decrypts(String value) {
        if (value == null) {
            return false;
        }
        try {
            dataEncryptor.decrypt(value);
            return true;
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    public List<Property> encryptRestoredValuesOfEncryptedKeys(List<Property> storedProperties, List<Property> restoredProperties) {
        if (storedProperties == null || restoredProperties == null) {
            return restoredProperties;
        }
        Set<String> encryptedKeys = storedProperties
            .stream()
            .filter(Objects::nonNull)
            .filter(Property::isEncrypted)
            .map(Property::getKey)
            .collect(Collectors.toSet());
        return encryptProperties(
            restoredProperties
                .stream()
                .filter(Objects::nonNull)
                .map(restored -> {
                    var encryptable = EncryptableProperty.fromProperty(restored);
                    encryptable.setEncryptable(encryptedKeys.contains(restored.getKey()));
                    return encryptable;
                })
                .toList()
        );
    }

    public List<Property> keepStoredEncryption(String apiId, List<Property> storedProperties, List<Property> fetchedProperties) {
        Map<String, Property> encryptedDynamicPropertiesByKey = storedProperties
            .stream()
            .filter(Objects::nonNull)
            .filter(Property::isDynamic)
            .filter(Property::isEncrypted)
            .collect(Collectors.toMap(Property::getKey, Function.identity(), (first, second) -> first));
        return fetchedProperties
            .stream()
            .map(fetched -> {
                var stored = encryptedDynamicPropertiesByKey.get(fetched.getKey());
                return stored == null ? fetched : reEncrypt(apiId, stored, fetched);
            })
            .toList();
    }

    private Property reEncrypt(String apiId, Property stored, Property fetched) {
        if (Objects.equals(decryptOrNull(apiId, stored), fetched.getValue())) {
            return stored;
        }
        try {
            return Property.builder()
                .key(fetched.getKey())
                .value(dataEncryptor.encrypt(fetched.getValue()))
                .encrypted(true)
                .dynamic(fetched.isDynamic())
                .build();
        } catch (GeneralSecurityException | RuntimeException e) {
            log.error("Unable to encrypt dynamic property [{}] of API [{}]; keeping the stored value", fetched.getKey(), apiId, e);
            return stored;
        }
    }

    private String decryptOrNull(String apiId, Property stored) {
        try {
            return dataEncryptor.decrypt(stored.getValue());
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.warn(
                "Stored value of encrypted dynamic property [{}] of API [{}] could not be decrypted; it will be encrypted again",
                stored.getKey(),
                apiId
            );
            return null;
        }
    }
}
