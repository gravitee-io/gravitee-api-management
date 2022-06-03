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
package io.gravitee.rest.api.service.impl.configuration.spel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.el.spel.context.SecuredMethodResolver;
import io.gravitee.rest.api.service.configuration.spel.SpelService;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class SpelServiceImpl implements SpelService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpelServiceImpl.class);

    // Generate with EvaluableExtractor
    // TODO: replace this file by direct call
    private static final String GRAMMAR_PATH = "/spel/grammar.json";

    private final ObjectMapper mapper;
    private final SecuredMethodResolver securedMethodResolver;

    public SpelServiceImpl(ObjectMapper mapper) {
        this.mapper = mapper;
        this.securedMethodResolver = new SecuredMethodResolver();
    }

    private static final List<Class<?>> SUPPORTED_TYPES = List.of(
        MultiValueMap.class,
        HttpHeaders.class,
        Map.class,
        Boolean.class,
        Integer.class,
        Long.class,
        Math.class,
        Object.class,
        List.class,
        Collection.class,
        Set.class,
        String.class,
        String[].class
    );

    @Override
    public JsonNode getGrammar() {
        try {
            InputStream resourceAsStream = this.getClass().getResourceAsStream(GRAMMAR_PATH);
            JsonNode actualObj = mapper.readTree(resourceAsStream);

            ObjectNode typesNode = ((ObjectNode) actualObj).putObject("_types");
            buildTypes(typesNode);

            return actualObj;
        } catch (IOException e) {
            LOGGER.error("Error while getting the Expression Language grammar", e);
        }
        return null;
    }

    private void buildTypes(ObjectNode types) {
        SUPPORTED_TYPES.forEach(
            aClass -> {
                ObjectNode type = types.putObject(aClass.getSimpleName());
                buildType(type, aClass);
            }
        );
    }

    private void buildType(ObjectNode type, Class<?> classz) {
        final ArrayNode methodsNode = type.putArray("methods");
        Arrays
            .stream(securedMethodResolver.getMethods(classz))
            .filter(f -> Modifier.isPublic(f.getModifiers()))
            .forEach(
                method -> {
                    ObjectNode methodNode = methodsNode.addObject();
                    fillMethod(methodNode, method);
                }
            );
    }

    private void fillMethod(ObjectNode methodNode, Method method) {
        methodNode.put("name", method.getName());
        methodNode.put("returnType", method.getReturnType().getSimpleName());

        final Parameter[] parameters = method.getParameters();
        if (parameters.length > 0) {
            final ArrayNode paramsNode = methodNode.putArray("params");
            Arrays
                .stream(parameters)
                .forEach(
                    parameter -> {
                        ObjectNode paramNode = paramsNode.addObject();
                        fillParameter(paramNode, parameter);
                    }
                );
        }
    }

    private void fillParameter(ObjectNode parameterNode, Parameter parameter) {
        parameterNode.put("name", parameter.getName());
        parameterNode.put("type", parameter.getType().getSimpleName());
    }
}
