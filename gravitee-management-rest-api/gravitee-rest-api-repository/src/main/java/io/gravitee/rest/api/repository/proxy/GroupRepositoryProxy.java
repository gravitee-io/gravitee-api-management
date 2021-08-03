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
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.model.Group;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class GroupRepositoryProxy extends AbstractProxy<GroupRepository> implements GroupRepository {

    @Override
    public Optional<Group> findById(String id) throws TechnicalException {
        return target.findById(id);
    }

    @Override
    public Group create(Group item) throws TechnicalException {
        return target.create(item);
    }

    @Override
    public Group update(Group item) throws TechnicalException {
        return target.update(item);
    }

    @Override
    public void delete(String s) throws TechnicalException {
        target.delete(s);
    }

    @Override
    public Set<Group> findAll() throws TechnicalException {
        return target.findAll();
    }

    @Override
    public Set<Group> findByIds(Set<String> ids) throws TechnicalException {
        return target.findByIds(ids);
    }

    @Override
    public Set<Group> findAllByEnvironment(String environment) throws TechnicalException {
        return target.findAllByEnvironment(environment);
    }

    @Override
    public Set<Group> findAllByOrganization(String organization) throws TechnicalException {
        return target.findAllByOrganization(organization);
    }
}
