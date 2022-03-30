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
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
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
    protected void deserialize(T service, JsonParser jsonParser, JsonNode node, DeserializationContext ctxt)
        throws IOException, JsonProcessingException {
        super.deserialize(service, jsonParser, node, ctxt);

        final JsonNode scheduleNode = node.get("schedule");
        String schedule;
        if (scheduleNode != null) {
            schedule = scheduleNode.asText();
        } else {
            Long rate = null;
            TimeUnit unit = null;
            if (node.has("trigger")) {
                final JsonNode triggerNode = node.get("trigger");
                final JsonNode rateNode = triggerNode.get("rate");

                if (rateNode != null) {
                    rate = rateNode.asLong();
                } else {
                    throw ctxt.mappingException("[scheduled-service] Rate is required");
                }

                final JsonNode unitNode = triggerNode.get("unit");
                if (unitNode != null) {
                    unit = TimeUnit.valueOf(unitNode.asText().toUpperCase());
                } else {
                    throw ctxt.mappingException("[scheduled-service] Unit is required");
                }
            } else if (node.has("interval")) {
                // Ensure backward compatibility
                final JsonNode intervalNode = node.get("interval");

                if (intervalNode != null) {
                    rate = intervalNode.asLong();

                    final JsonNode unitNode = node.get("unit");
                    if (unitNode != null) {
                        unit = TimeUnit.valueOf(unitNode.asText().toUpperCase());
                    } else {
                        throw ctxt.mappingException("[scheduled-service] Unit is required");
                    }
                }
            }

            schedule = convertToCron(rate, unit);
        }

        service.setSchedule(schedule);
    }

    public static String convertToCron(Long rate, TimeUnit unit) {
        if (rate == null || unit == null) {
            return null;
        }
        String schedule;
        List<String> scheduleList = Arrays.asList("*", "*", "*", "*", "*", "*");
        List<TimeUnit> timeUnits = Arrays.asList(TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS);
        int index = timeUnits.indexOf(unit);
        for (int i = 0; i < index; i++) {
            scheduleList.set(i, "0");
        }
        scheduleList.set(index, "*/" + rate);
        schedule = String.join(" ", scheduleList);
        return schedule;
    }
}
