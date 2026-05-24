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
package io.gravitee.gamma.authorization.infra.repository.document;

import io.gravitee.gamma.authorization.domain.AuthzPolicyKind;
import io.gravitee.gamma.authorization.domain.AuthzPolicyStatus;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "#{@environment.getProperty('management.mongodb.prefix')}authz_policies")
public record AuthzPolicyMongo(
    @Id String id,
    String name,
    AuthzPolicyKind kind,
    String entityId,
    String policyText,
    AuthzPolicyStatus status,
    String environmentId,
    Instant createdAt,
    Instant updatedAt
) {}
