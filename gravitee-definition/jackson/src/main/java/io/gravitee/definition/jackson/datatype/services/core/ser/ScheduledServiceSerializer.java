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
package io.gravitee.definition.jackson.datatype.services.core.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.gravitee.definition.model.services.schedule.ScheduledService;
import io.gravitee.definition.model.services.schedule.Trigger;

import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class ScheduledServiceSerializer<T extends ScheduledService> extends ServiceSerializer<T> {

    protected ScheduledServiceSerializer(Class<T> t) {
        super(t);
    }

    @Override
    protected void doSerialize(T service, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {
        super.doSerialize(service, jgen, serializerProvider);

        Trigger trigger = service.getTrigger();
        if (trigger != null) {
            jgen.writeObjectFieldStart("trigger");
            jgen.writeNumberField("rate", trigger.getRate());
            jgen.writeStringField("unit", trigger.getUnit().toString());
            jgen.writeEndObject();
        }
    }
}
