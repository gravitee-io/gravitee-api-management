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
package io.gravitee.definition.jackson.datatype.services.dynamicproperty.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.gravitee.definition.jackson.datatype.services.core.ser.ScheduledServiceSerializer;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService;
import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DynamicPropertySerializer extends ScheduledServiceSerializer<DynamicPropertyService> {

    public DynamicPropertySerializer(Class<DynamicPropertyService> t) {
        super(t);
    }

    @Override
    protected void doSerialize(DynamicPropertyService service, JsonGenerator jgen, SerializerProvider serializerProvider)
        throws IOException {
        super.doSerialize(service, jgen, serializerProvider);

        jgen.writeStringField("provider", service.getProvider().name());
        jgen.writeObjectField("configuration", service.getConfiguration());
    }
}
