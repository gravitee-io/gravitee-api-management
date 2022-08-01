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
package io.gravitee.rest.api.service.v4.impl;

import io.gravitee.common.util.DataEncryptor;
import io.gravitee.rest.api.model.v4.api.properties.PropertyEntity;
import io.gravitee.rest.api.service.impl.TransactionalService;
import io.gravitee.rest.api.service.v4.PropertiesService;
import java.security.GeneralSecurityException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Component
public class PropertiesServiceImpl extends TransactionalService implements PropertiesService {

    private final DataEncryptor dataEncryptor;

    public PropertiesServiceImpl(final DataEncryptor dataEncryptor) {
        this.dataEncryptor = dataEncryptor;
    }

    @Override
    public List<PropertyEntity> encryptProperties(List<PropertyEntity> properties) {
        for (PropertyEntity property : properties) {
            if (property.isEncryptable() && !property.isEncrypted()) {
                try {
                    property.setValue(dataEncryptor.encrypt(property.getValue()));
                    property.setEncrypted(true);
                } catch (GeneralSecurityException e) {
                    log.error("Error encrypting property value", e);
                }
            }
        }
        return properties;
    }
}
