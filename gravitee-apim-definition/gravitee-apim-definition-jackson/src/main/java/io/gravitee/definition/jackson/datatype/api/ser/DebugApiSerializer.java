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
package io.gravitee.definition.jackson.datatype.api.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.debug.DebugApiV2;
import java.io.IOException;

public class DebugApiSerializer extends StdScalarSerializer<DebugApiV2> {

    private final ApiSerializer base;

    public DebugApiSerializer(Class<DebugApiV2> t) {
        super(t);
        this.base = new ApiSerializer(Api.class);
    }

    @Override
    public void serialize(DebugApiV2 api, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        this.base.serializeContent(api, jgen, provider);

        if (api.getRequest() != null) {
            jgen.writeObjectField("request", api.getRequest());
        }

        if (api.getResponse() != null) {
            jgen.writeObjectField("response", api.getResponse());
        }

        if (api.getDebugSteps() != null) {
            jgen.writeObjectField("debugSteps", api.getDebugSteps());
        }

        if (api.getPreprocessorStep() != null) {
            jgen.writeObjectField("preprocessorStep", api.getPreprocessorStep());
        }

        if (api.getBackendResponse() != null) {
            jgen.writeObjectField("backendResponse", api.getBackendResponse());
        }

        if (api.getMetrics() != null) {
            jgen.writeObjectField("metrics", api.getMetrics());
        }

        jgen.writeEndObject();
    }
}
