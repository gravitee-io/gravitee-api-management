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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.*;
import io.gravitee.rest.api.model.JsonPatch;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.exceptions.JsonPatchTestFailedException;
import io.gravitee.rest.api.service.exceptions.JsonPatchUnsafeException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.sanitizer.HtmlSanitizer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
public class JsonPatchServiceImpl extends AbstractService implements JsonPatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonPatchServiceImpl.class);

    private static final Configuration CONFIGURATION;

    private static final Configuration TEST_CONFIGURATION;

    static {
        CONFIGURATION = Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);
        TEST_CONFIGURATION = Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS);
    }

    private final ObjectMapper objectMapper;

    public JsonPatchServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String execute(String json, Collection<JsonPatch> jsonPatches) {
        try {
            final DocumentContext apiDefinitionParsed = JsonPath.parse(json);
            Object apiDefinitionToUpdate = transform(jsonPatches, apiDefinitionParsed.json());
            return objectMapper.writeValueAsString(apiDefinitionToUpdate);
        } catch (JsonProcessingException | InvalidPathException ex) {
            LOGGER.error("An error occurs while trying to execute json patches", ex);
            throw new TechnicalManagementException("An error occurs while trying to execute json patches", ex);
        }
    }

    private Object transform(Collection<JsonPatch> jsonPatches, Object apiDefinitionToUpdate) {
        for (JsonPatch jsonPatch : jsonPatches) {
            apiDefinitionToUpdate = this.execute(apiDefinitionToUpdate, jsonPatch);
        }
        return apiDefinitionToUpdate;
    }

    private Object execute(Object jsonObject, JsonPatch jsonPatch) {
        String jsonPath = jsonPatch.getJsonPath();
        checkSafe(jsonPath);
        Object value = jsonPatch.getValue();
        checkSafe(value);

        DocumentContext parse = JsonPath.parse(jsonObject);
        Object json = parse.json();

        JsonPath compile = JsonPath.compile(jsonPath);
        if (JsonPatch.Operation.REPLACE.equals(jsonPatch.getOperation())) {
            return compile.set(json, value, CONFIGURATION);
        } else if (JsonPatch.Operation.ADD.equals(jsonPatch.getOperation())) {
            return compile.add(json, value, CONFIGURATION);
        } else if (JsonPatch.Operation.REMOVE.equals(jsonPatch.getOperation())) {
            return compile.delete(json, CONFIGURATION);
        } else if (JsonPatch.Operation.TEST.equals(jsonPatch.getOperation())) {
            Object read = compile.read(json, TEST_CONFIGURATION);
            if (!(read == null && "null".equals(value) || read != null && read.equals(value))) {
                throw new JsonPatchTestFailedException(jsonPatch);
            }
        }
        return json;
    }

    private void checkSafe(Object value) {
        if (value instanceof Map) {
            ((Map) value).keySet().forEach(this::checkSafe);
            ((Map) value).values().forEach(this::checkSafe);
        } else if (value instanceof List) {
            ((List) value).forEach(this::checkSafe);
        } else if (value != null) {
            HtmlSanitizer.SanitizeInfos sanitizeInfos = HtmlSanitizer.isSafe(value.toString());
            if (!sanitizeInfos.isSafe()) {
                throw new JsonPatchUnsafeException(sanitizeInfos.getRejectedMessage());
            }
        }
    }
}
