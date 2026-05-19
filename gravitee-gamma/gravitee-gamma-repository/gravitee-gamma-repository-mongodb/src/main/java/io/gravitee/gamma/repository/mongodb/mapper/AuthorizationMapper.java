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
package io.gravitee.gamma.repository.mongodb.mapper;

import io.gravitee.gamma.repository.authorization.model.AuthorizationEntity;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicy;
import io.gravitee.gamma.repository.mongodb.internal.model.AuthorizationEntityMongo;
import io.gravitee.gamma.repository.mongodb.internal.model.AuthorizationPolicyMongo;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthorizationMapper {
    AuthorizationEntity map(AuthorizationEntityMongo mongo);

    AuthorizationEntityMongo map(AuthorizationEntity domain);

    AuthorizationPolicy map(AuthorizationPolicyMongo mongo);

    AuthorizationPolicyMongo map(AuthorizationPolicy domain);
}
