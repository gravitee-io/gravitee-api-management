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
package io.gravitee.rest.api.management.v2.rest.mapper;

import io.gravitee.apim.core.application.model.crd.ApplicationCRDSpec;
import io.gravitee.rest.api.management.v2.rest.model.BaseApplication;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import java.util.Collection;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper(uses = { DateMapper.class })
public interface ApplicationMapper {
    ApplicationMapper INSTANCE = Mappers.getMapper(ApplicationMapper.class);

    BaseApplication mapToBaseApplication(ApplicationListItem applicationListItem);
    List<BaseApplication> mapToBaseApplicationList(Collection<ApplicationListItem> applications);

    @Mapping(target = "picture", source = "pictureUrl")
    @Mapping(target = "disableMembershipNotifications", expression = "java(!spec.isNotifyMembers())")
    ApplicationCRDSpec map(io.gravitee.rest.api.management.v2.rest.model.ApplicationCRDSpec spec);
}
