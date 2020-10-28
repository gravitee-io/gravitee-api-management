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
package io.gravitee.rest.api.repository.proxy;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.NotificationTemplateRepository;
import io.gravitee.repository.management.model.NotificationTemplate;
import io.gravitee.repository.management.model.NotificationTemplateReferenceType;
import io.gravitee.repository.management.model.NotificationTemplateType;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class NotificationTemplateRepositoryProxy extends AbstractProxy<NotificationTemplateRepository> implements NotificationTemplateRepository {

    @Override
    public Optional<NotificationTemplate> findById(String id) throws TechnicalException {
        return target.findById(id);
    }

    @Override
    public NotificationTemplate create(NotificationTemplate item) throws TechnicalException {
        return target.create(item);
    }

    @Override
    public NotificationTemplate update(NotificationTemplate item) throws TechnicalException {
        return target.update(item);
    }

    @Override
    public void delete(String id) throws TechnicalException {
        target.delete(id);
    }

    @Override
    public Set<NotificationTemplate> findAll() throws TechnicalException {
        return target.findAll();
    }

    @Override
    public Set<NotificationTemplate> findAllByReferenceIdAndReferenceType(String referenceId, NotificationTemplateReferenceType referenceType) throws TechnicalException {
        return target.findAllByReferenceIdAndReferenceType(referenceId, referenceType);
    }

    @Override
    public Set<NotificationTemplate> findByHookAndScopeAndReferenceIdAndReferenceType(String hook, String scope, String referenceId, NotificationTemplateReferenceType referenceType) throws TechnicalException {
        return target.findByHookAndScopeAndReferenceIdAndReferenceType(hook, scope, referenceId, referenceType);
    }

    @Override
    public Set<NotificationTemplate> findByTypeAndReferenceIdAndReferenceType(NotificationTemplateType type, String referenceId, NotificationTemplateReferenceType referenceType) throws TechnicalException {
        return target.findByTypeAndReferenceIdAndReferenceType(type, referenceId, referenceType);
    }
}
