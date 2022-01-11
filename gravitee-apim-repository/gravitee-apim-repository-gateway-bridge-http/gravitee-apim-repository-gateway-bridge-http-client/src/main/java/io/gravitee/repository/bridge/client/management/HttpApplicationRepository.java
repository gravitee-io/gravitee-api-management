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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.search.ApplicationCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class HttpApplicationRepository extends AbstractRepository implements ApplicationRepository {

    @Override
    public Optional<Application> findById(String s) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Application create(Application item) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Application update(Application item) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public void delete(String s) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<Application> findAll(ApplicationStatus... statuses) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<Application> findByIds(List<String> ids) {
        throw new IllegalStateException();
    }

    @Override
    public Set<Application> findByIds(List<String> ids, Sortable sortable) {
        throw new IllegalStateException();
    }

    @Override
    public Set<Application> findByGroups(List<String> groupIds, ApplicationStatus... statuses) {
        throw new IllegalStateException();
    }

    @Override
    public Set<Application> findAllByEnvironment(String environmentId, ApplicationStatus... statuses) {
        throw new IllegalStateException();
    }

    @Override
    public Set<Application> findByNameAndStatuses(String partialName, ApplicationStatus... statuses) throws TechnicalException {
        throw new TechnicalException();
    }

    @Override
    public Page<Application> search(ApplicationCriteria applicationCriteria, Pageable pageable) throws TechnicalException {
        throw new TechnicalException();
    }

    @Override
    public Set<Application> findAll() throws TechnicalException {
        throw new IllegalStateException();
    }
}
