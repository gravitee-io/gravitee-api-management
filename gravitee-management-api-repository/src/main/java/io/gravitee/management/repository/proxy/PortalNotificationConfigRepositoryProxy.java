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
package io.gravitee.management.repository.proxy;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.model.PortalNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PortalNotificationConfigRepositoryProxy extends AbstractProxy<PortalNotificationConfigRepository> implements PortalNotificationConfigRepository {

    @Override
    public PortalNotificationConfig create(PortalNotificationConfig portalNotificationConfig) throws TechnicalException {
        return target.create(portalNotificationConfig);
    }

    @Override
    public PortalNotificationConfig update(PortalNotificationConfig portalNotificationConfig) throws TechnicalException {
        return target.update(portalNotificationConfig);
    }

    @Override
    public void delete(PortalNotificationConfig portalNotificationConfig) throws TechnicalException {
        target.delete(portalNotificationConfig);
    }

    @Override
    public Optional<PortalNotificationConfig> findById(String user, NotificationReferenceType referenceType, String referenceId) throws TechnicalException {
        return target.findById(user, referenceType, referenceId);
    }

    @Override
    public List<PortalNotificationConfig> findByReferenceAndHook(String hook, NotificationReferenceType referenceType, String referenceId) throws TechnicalException {
        return target.findByReferenceAndHook(hook, referenceType, referenceId);
    }
}
