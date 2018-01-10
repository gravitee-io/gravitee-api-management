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
import io.gravitee.repository.management.api.PortalNotificationRepository;
import io.gravitee.repository.management.model.PortalNotification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PortalNotificationRepositoryProxy extends AbstractProxy<PortalNotificationRepository> implements PortalNotificationRepository {

    @Override
    public Optional<PortalNotification> findById(String s) throws TechnicalException {
        return target.findById(s);
    }

    @Override
    public void create(List<PortalNotification> items) throws TechnicalException {
        target.create(items);
    }

    @Override
    public PortalNotification update(PortalNotification item) throws TechnicalException {
        return target.update(item);
    }

    @Override
    public List<PortalNotification> findByUser(String user) throws TechnicalException {
        return target.findByUser(user);
    }

    @Override
    public PortalNotification create(PortalNotification item) throws TechnicalException {
        return target.create(item);
    }

    @Override
    public void delete(String s) throws TechnicalException {
        target.delete(s);
    }

    @Override
    public void deleteAll(String s) throws TechnicalException {
        target.deleteAll(s);
    }
}
