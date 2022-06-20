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
package io.gravitee.repository.bridge.client.management;

import io.gravitee.repository.bridge.client.utils.ExcludeMethodFromGeneratedCoverage;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalNotificationRepository;
import io.gravitee.repository.management.model.PortalNotification;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class HttpPortalNotificationRepository extends AbstractRepository implements PortalNotificationRepository {

    @Override
    @ExcludeMethodFromGeneratedCoverage
    public PortalNotification create(PortalNotification item) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    @ExcludeMethodFromGeneratedCoverage
    public void delete(String s) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    @ExcludeMethodFromGeneratedCoverage
    public List<PortalNotification> findByUser(String user) {
        throw new IllegalStateException();
    }

    @Override
    @ExcludeMethodFromGeneratedCoverage
    public void create(List<PortalNotification> notifications) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    @ExcludeMethodFromGeneratedCoverage
    public void deleteAll(String user) {
        throw new IllegalStateException();
    }

    @Override
    @ExcludeMethodFromGeneratedCoverage
    public Optional<PortalNotification> findById(String id) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    @ExcludeMethodFromGeneratedCoverage
    public Set<PortalNotification> findAll() throws TechnicalException {
        throw new IllegalStateException();
    }
}
