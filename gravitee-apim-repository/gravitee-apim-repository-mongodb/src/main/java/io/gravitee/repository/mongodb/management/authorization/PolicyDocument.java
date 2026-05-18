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
package io.gravitee.repository.mongodb.management.authorization;

import io.gravitee.apim.authorization.domain.Policy;
import io.gravitee.apim.authorization.domain.PolicyKind;
import io.gravitee.apim.authorization.domain.PolicyStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "authz_policies")
public class PolicyDocument {

    @Id
    private String id;

    @Field("name")
    private String name;

    @Field("kind")
    private PolicyKind kind;

    @Field("entityId")
    private String entityId;

    @Field("policyText")
    private String policyText;

    @Field("status")
    private PolicyStatus status;

    @Field("environmentId")
    private String environmentId;

    @Field("createdAt")
    private Instant createdAt;

    @Field("updatedAt")
    private Instant updatedAt;

    public static PolicyDocument fromDomain(Policy policy) {
        return new PolicyDocument(
            policy.id(),
            policy.name(),
            policy.kind(),
            policy.entityId(),
            policy.policyText(),
            policy.status(),
            policy.environmentId(),
            policy.createdAt(),
            policy.updatedAt()
        );
    }

    public Policy toDomain() {
        return new Policy(id, name, kind, entityId, policyText, status, environmentId, createdAt, updatedAt);
    }
}
