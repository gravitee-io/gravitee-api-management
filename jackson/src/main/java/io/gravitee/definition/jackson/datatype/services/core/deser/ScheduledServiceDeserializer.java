/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.definition.jackson.datatype.services.core.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.model.services.schedule.ScheduledService;
import io.gravitee.definition.model.services.schedule.Trigger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class ScheduledServiceDeserializer<T extends ScheduledService> extends ServiceDeserializer<T> {

    public ScheduledServiceDeserializer(Class<T> vc) {
        super(vc);
    }

    @Override
    protected void deserialize(T service, JsonParser jsonParser, JsonNode node, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        super.deserialize(service, jsonParser, node, ctxt);

        // New version of scheduler service
        final JsonNode triggerNode = node.get("trigger");

        if (triggerNode != null) {
            Trigger trigger = new Trigger();
            final JsonNode rateNode = triggerNode.get("rate");

            if (rateNode != null) {
                trigger.setRate(rateNode.asLong());
            } else {
                throw ctxt.mappingException("[scheduled-service] Rate is required");
            }

            final JsonNode unitNode = triggerNode.get("unit");
            if (unitNode != null) {
                trigger.setUnit(TimeUnit.valueOf(unitNode.asText().toUpperCase()));
            } else {
                throw ctxt.mappingException("[scheduled-service] Unit is required");
            }
            service.setTrigger(trigger);
        } else {
            // Ensure backward compatibility
            final JsonNode intervalNode = node.get("interval");

            if (intervalNode != null) {
                Trigger trigger = new Trigger();
                trigger.setRate(intervalNode.asLong());

                final JsonNode unitNode = node.get("unit");
                if (unitNode != null) {
                    trigger.setUnit(TimeUnit.valueOf(unitNode.asText().toUpperCase()));
                } else {
                    throw ctxt.mappingException("[scheduled-service] Unit is required");
                }
                service.setTrigger(trigger);
            }
        }

    }
}
