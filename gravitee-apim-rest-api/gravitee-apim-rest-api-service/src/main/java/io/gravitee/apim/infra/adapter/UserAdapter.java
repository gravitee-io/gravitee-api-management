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

import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.core.user.model.IdpSource;
import io.gravitee.repository.management.model.User;
import io.gravitee.rest.api.model.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper
public interface UserAdapter {
    UserAdapter INSTANCE = Mappers.getMapper(UserAdapter.class);

    @Mapping(target = "source", source = "source", qualifiedByName = "stringToIdpSource")
    BaseUserEntity fromUser(User user);

    @Mapping(target = "source", source = "source", qualifiedByName = "stringToIdpSource")
    BaseUserEntity fromUser(UserEntity user);

    @Mapping(target = "source", source = "source", qualifiedByName = "idpSourceToString")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "envRoles", ignore = true)
    @Mapping(target = "picture", ignore = true)
    @Mapping(target = "lastConnectionAt", ignore = true)
    @Mapping(target = "firstConnectionAt", ignore = true)
    @Mapping(target = "primaryOwner", ignore = true)
    @Mapping(target = "loginCount", ignore = true)
    @Mapping(target = "nbActiveTokens", ignore = true)
    @Mapping(target = "newsletterSubscribed", ignore = true)
    @Mapping(target = "isServiceAccount", ignore = true)
    @Mapping(target = "customFields", ignore = true)
    @Mapping(target = "hasPassword", ignore = true)
    @Mapping(target = "referenceType", ignore = true)
    @Mapping(target = "referenceId", ignore = true)
    UserEntity toUserEntity(BaseUserEntity base);

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "lastConnectionAt", ignore = true)
    @Mapping(target = "firstConnectionAt", ignore = true)
    @Mapping(target = "picture", ignore = true)
    @Mapping(target = "loginCount", ignore = true)
    @Mapping(target = "newsletterSubscribed", ignore = true)
    @Mapping(target = "isServiceAccount", ignore = true)
    @Mapping(target = "source", source = "source", qualifiedByName = "idpSourceToString")
    User toUser(BaseUserEntity base);

    @Named("stringToIdpSource")
    default IdpSource stringToIdpSource(String source) {
        return IdpSource.of(source);
    }

    @Named("idpSourceToString")
    default String idpSourceToString(IdpSource source) {
        return source != null ? source.value() : null;
    }
}
