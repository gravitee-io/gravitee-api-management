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
package io.gravitee.apim.infra.adapter;

import io.gravitee.apim.core.group.model.Group;
import io.gravitee.repository.management.model.GroupEvent;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface GroupAdapter {
    GroupAdapter INSTANCE = Mappers.getMapper(GroupAdapter.class);

    Group toModel(io.gravitee.repository.management.model.Group group);

    io.gravitee.repository.management.model.Group toRepository(Group group);

    Group.GroupEvent mapEvent(GroupEvent source);
    GroupEvent mapEvent(Group.GroupEvent source);
}
