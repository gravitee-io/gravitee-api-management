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
package io.gravitee.gateway.handlers.guardrail.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.v4.guardrail.Guardrail;
import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.api.genericevent.GenericEventSubscriber;
import io.gravitee.gateway.handlers.guardrail.ReactableGuardrail;
import io.gravitee.gateway.handlers.guardrail.manager.GuardrailManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Date;
import java.util.Objects;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Deploys the Guardrails published as generic events of kind {@value #GUARDRAIL_KIND}, whose payload is the {@link Guardrail}
 * definition. The event identifies the Guardrail: its id and environment override those of the definition.
 *
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
public class GuardrailGenericEventSubscriber implements GenericEventSubscriber {

    public static final String GUARDRAIL_KIND = "guardrail.definition";

    /**
     * Shared with the Guardrail reactor plugin, which counts the definitions it refuses to start: an event refused here
     * never reaches it, and would otherwise be counted nowhere.
     */
    static final String DEPLOYMENT_REJECTIONS = "guardrail_deployment_rejections";

    private final GuardrailManager guardrailManager;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Override
    public void onUpsert(GenericEvent event) {
        if (event.updatedAt() == null) {
            // The update date is the revision: without it, nothing tells a newer definition from a replayed one.
            log.error(
                "Unable to deploy Guardrail [{}] of environment [{}]: its event has no update date",
                event.id(),
                event.environmentId()
            );
            countRejection(event);
            return;
        }
        Guardrail definition;
        try {
            definition = objectMapper.readValue(event.payload(), Guardrail.class);
        } catch (JsonProcessingException e) {
            // An already deployed revision keeps running: undeploying it would turn a publication error into an outage.
            log.error("Unable to read the definition of Guardrail [{}] of environment [{}]", event.id(), event.environmentId(), e);
            countRejection(event);
            return;
        }
        if (definition.getPhase() == null) {
            // Jackson does not enforce required properties on this bean: an incomplete definition would replace the
            // running revision with one failing every call.
            log.error(
                "Unable to deploy Guardrail [{}] of environment [{}]: its definition has no phase",
                event.id(),
                event.environmentId()
            );
            countRejection(event);
            return;
        }
        guardrailManager.register(
            ReactableGuardrail.builder()
                .definition(definition.toBuilder().id(event.id()).environmentId(event.environmentId()).build())
                .organizationId(event.organizationId())
                .deployedAt(Date.from(event.updatedAt()))
                // A generic event carries no deployment number: its update date is what changes on every deployment.
                .revision(String.valueOf(event.updatedAt().toEpochMilli()))
                .build()
        );
    }

    private void countRejection(GenericEvent event) {
        Counter.builder(DEPLOYMENT_REJECTIONS)
            .description("Guardrail revisions the gateway refused to deploy because their definition is invalid")
            .tag("environment", Objects.requireNonNullElse(event.environmentId(), "unknown"))
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void onRemove(GenericEvent event) {
        guardrailManager.unregister(event.id(), event.environmentId());
    }
}
