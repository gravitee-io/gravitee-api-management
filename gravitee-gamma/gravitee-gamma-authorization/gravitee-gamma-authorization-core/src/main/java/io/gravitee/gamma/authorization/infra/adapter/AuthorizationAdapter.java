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
package io.gravitee.gamma.authorization.infra.adapter;

import io.gravitee.gamma.authorization.domain.Entity;
import io.gravitee.gamma.authorization.domain.EntityKind;
import io.gravitee.gamma.authorization.domain.Policy;
import io.gravitee.gamma.authorization.domain.PolicyKind;
import io.gravitee.gamma.authorization.domain.PolicyStatus;
import io.gravitee.gamma.repository.authorization.model.AuthorizationEntity;
import io.gravitee.gamma.repository.authorization.model.AuthorizationEntityKind;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicy;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicyKind;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicyStatus;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthorizationAdapter {
    Entity toDomain(AuthorizationEntity stored);

    AuthorizationEntity toStorage(Entity domain);

    Policy toDomain(AuthorizationPolicy stored);

    AuthorizationPolicy toStorage(Policy domain);

    EntityKind toDomain(AuthorizationEntityKind stored);

    AuthorizationEntityKind toStorage(EntityKind domain);

    PolicyKind toDomain(AuthorizationPolicyKind stored);

    AuthorizationPolicyKind toStorage(PolicyKind domain);

    PolicyStatus toDomain(AuthorizationPolicyStatus stored);

    AuthorizationPolicyStatus toStorage(PolicyStatus domain);
}
