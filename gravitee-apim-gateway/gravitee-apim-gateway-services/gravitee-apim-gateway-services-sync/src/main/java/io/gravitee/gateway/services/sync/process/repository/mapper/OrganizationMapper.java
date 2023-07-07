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
package io.gravitee.gateway.services.sync.process.repository.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.Organization;
import io.gravitee.repository.management.model.Event;
import io.reactivex.rxjava3.core.Maybe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class OrganizationMapper {

    private final ObjectMapper objectMapper;

    public Maybe<io.gravitee.definition.model.Organization> to(final Event event) {
        return Maybe.fromCallable(() -> {
            try {
                // Read organization definition from event
                final Organization organization = objectMapper.readValue(event.getPayload(), Organization.class);
                organization.setUpdatedAt(event.getUpdatedAt());
                return organization;
            } catch (Exception e) {
                log.warn("Error while determining deployed organization into events payload", e);
                return null;
            }
        });
    }
}
