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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.rest.api.service.validator.JsonHelper.clearNullValues;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import io.gravitee.rest.api.service.JsonSchemaService;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class JsonSchemaServiceImpl implements JsonSchemaService {

    @Autowired
    private JsonSchemaFactory jsonSchemaFactory;

    @Override
    public String validate(String schema, String configuration) {
        try {
            // At least, validate json.
            String safeConfiguration = clearNullValues(configuration);
            JsonNode jsonConfiguration = JsonLoader.fromString(safeConfiguration);

            if (schema != null && !schema.equals("")) {
                // Validate json against schema when defined.
                JsonNode jsonSchema = JsonLoader.fromString(schema);
                ListProcessingReport report = (ListProcessingReport) jsonSchemaFactory
                    .getValidator()
                    .validate(jsonSchema, jsonConfiguration, true);
                if (!report.isSuccess()) {
                    boolean hasDefaultValue = false;
                    String msg = "";
                    if (report.iterator().hasNext()) {
                        msg = " : " + report.iterator().next().getMessage();
                        Pattern pattern = Pattern.compile("\\(\\[\\\"(.*?)\\\"\\]\\)");
                        Matcher matcher = pattern.matcher(msg);
                        if (matcher.find()) {
                            String field = matcher.group(1);
                            JsonNode properties = jsonSchema.get("properties");
                            hasDefaultValue =
                                properties != null && properties.get(field) != null && properties.get(field).get("default") != null;
                        }
                    }
                    if (!hasDefaultValue) {
                        throw new InvalidDataException("Invalid configuration" + msg);
                    }
                }
            }

            return safeConfiguration;
        } catch (IOException | ProcessingException e) {
            throw new InvalidDataException("Unable to validate configuration", e);
        }
    }
}
