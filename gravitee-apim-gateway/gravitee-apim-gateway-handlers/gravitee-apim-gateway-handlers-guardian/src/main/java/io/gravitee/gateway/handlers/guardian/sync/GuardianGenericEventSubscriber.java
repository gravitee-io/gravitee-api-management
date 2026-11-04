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
package io.gravitee.gateway.handlers.guardian.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.v4.guardian.Guardian;
import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.api.genericevent.GenericEventSubscriber;
import io.gravitee.gateway.handlers.guardian.ReactableGuardian;
import io.gravitee.gateway.handlers.guardian.manager.GuardianManager;
import java.util.Date;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Deploys the Guardians published as generic events of kind {@value #GUARDIAN_KIND}, whose payload is the {@link Guardian}
 * definition. The event identifies the Guardian: its id and environment override those of the definition.
 *
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
public class GuardianGenericEventSubscriber implements GenericEventSubscriber {

    public static final String GUARDIAN_KIND = "guardian.definition";

    private final GuardianManager guardianManager;
    private final ObjectMapper objectMapper;

    @Override
    public void onUpsert(GenericEvent event) {
        if (event.updatedAt() == null) {
            // The update date is the revision: without it, nothing tells a newer definition from a replayed one.
            log.error(
                "Unable to deploy Guardian [{}] of environment [{}]: its event has no update date",
                event.id(),
                event.environmentId()
            );
            return;
        }
        Guardian definition;
        try {
            definition = objectMapper.readValue(event.payload(), Guardian.class);
        } catch (JsonProcessingException e) {
            // An already deployed revision keeps running: undeploying it would turn a publication error into an outage.
            log.error("Unable to read the definition of Guardian [{}] of environment [{}]", event.id(), event.environmentId(), e);
            return;
        }
        if (definition.getPhase() == null) {
            // Jackson does not enforce required properties on this bean: an incomplete definition would replace the
            // running revision with one failing every call.
            log.error("Unable to deploy Guardian [{}] of environment [{}]: its definition has no phase", event.id(), event.environmentId());
            return;
        }
        guardianManager.register(
            ReactableGuardian.builder()
                .definition(definition.toBuilder().id(event.id()).environmentId(event.environmentId()).build())
                .organizationId(event.organizationId())
                .deployedAt(Date.from(event.updatedAt()))
                // A generic event carries no deployment number: its update date is what changes on every deployment.
                .revision(String.valueOf(event.updatedAt().toEpochMilli()))
                .build()
        );
    }

    @Override
    public void onRemove(GenericEvent event) {
        guardianManager.unregister(event.id(), event.environmentId());
    }
}
