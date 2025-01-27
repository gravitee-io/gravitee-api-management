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
package io.gravitee.apim.infra.crud_service.group;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.group.crud_service.GroupCrudService;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.infra.adapter.GroupAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GroupRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Service
public class GroupCrudServiceImpl implements GroupCrudService {

    private final GroupRepository groupRepository;

    public GroupCrudServiceImpl(@Lazy GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    @Override
    public Group create(Group group) {
        try {
            var created = groupRepository.create(GroupAdapter.INSTANCE.toRepository(group));
            return GroupAdapter.INSTANCE.toModel(created);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Unable to create group " + group.getId(), e);
        }
    }

    @Override
    public Group update(Group group) {
        try {
            var updated = groupRepository.update(GroupAdapter.INSTANCE.toRepository(group));
            return GroupAdapter.INSTANCE.toModel(updated);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Unable to update group " + group.getId(), e);
        }
    }
}
