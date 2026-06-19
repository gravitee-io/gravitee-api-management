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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.authz;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import org.junit.jupiter.api.Test;

class AuthzPolicyMapperTargetPdpIdsTest {

    private final AuthzPolicyMapper mapper = new AuthzPolicyMapper(new ObjectMapper());

    private Event publishEvent(String payload) {
        Event e = new Event();
        e.setType(EventType.PUBLISH_AUTHZ_POLICY);
        e.setPayload(payload);
        return e;
    }

    @Test
    void toDeploy_carries_targetPdpIds_from_payload() {
        Event e = publishEvent(
            "{\"id\":\"p1\",\"name\":\"n\",\"kind\":\"GLOBAL\"," +
                "\"policyText\":\"permit(principal, action, resource);\"," +
                "\"targetPdpIds\":[\"api-a\",\"api-b\"]}"
        );
        AuthzPolicyReactorDeployable d = mapper.toDeploy(e).blockingGet();
        assertThat(d.targetPdpIds()).containsExactlyInAnyOrder("api-a", "api-b");
    }

    @Test
    void toDeploy_defaults_to_empty_scope_when_absent() {
        Event e = publishEvent(
            "{\"id\":\"p1\",\"name\":\"n\",\"kind\":\"GLOBAL\"," + "\"policyText\":\"permit(principal, action, resource);\"}"
        );
        AuthzPolicyReactorDeployable d = mapper.toDeploy(e).blockingGet();
        assertThat(d.targetPdpIds()).isEmpty();
    }
}
