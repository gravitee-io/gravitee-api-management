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

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.el.spel.context.SecuredMethodResolver;
import io.gravitee.rest.api.service.configuration.spel.SpelService;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class SpelServiceImpl implements SpelService {

    // Generate with EvaluableExtractor
    // TODO: replace this file by direct call
    private static final String GRAMMAR_PATH = "/spel/grammar.json";

    SecuredMethodResolver securedMethodResolver = new SecuredMethodResolver();

    private List<Class> supportedTypes = new ArrayList() {
        {
            add(MultiValueMap.class);
            add(HttpHeaders.class);
            add(Map.class);
            add(Boolean.class);
            add(Integer.class);
            add(Long.class);
            add(Math.class);
            add(Object.class);
            add(List.class);
            add(Collection.class);
            add(Set.class);
            add(String.class);
            add(String[].class);
        }
    };

    @Override
    public JSONObject getGrammar() {
        try {
            JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
            InputStream resourceAsStream = this.getClass().getResourceAsStream(GRAMMAR_PATH);
            JSONObject parse = (JSONObject) parser.parse(resourceAsStream);
            Map<String, Object> types = buildTypes();
            parse.appendField("_types", types);
            return parse;
        } catch (ParseException | UnsupportedEncodingException e) {}
        return null;
    }

    private Map<String, Object> buildTypes() {
        Map<String, Object> types = new HashMap<>();
        supportedTypes.forEach(aClass -> types.put(aClass.getSimpleName(), buildType(aClass)));
        return types;
    }

    private Map<String, Object> buildType(Class<?> classz) {
        Map type = new HashMap<>();

        List<Object> methods = Arrays
            .stream(securedMethodResolver.getMethods(classz))
            .filter(f -> Modifier.isPublic(f.getModifiers()))
            .map((Function<Method, Object>) method -> new MethodWrapper(method))
            .collect(Collectors.toList());
        type.put("methods", methods);
        return type;
    }

    private static class MethodWrapper extends HashMap {

        public MethodWrapper(Method method) {
            this.put("name", method.getName());
            this.put("returnType", method.getReturnType().getSimpleName());
            List<Object> params = Arrays
                .stream(method.getParameters())
                .map((Function<Parameter, Object>) parameter -> new ParameterWrapper(parameter))
                .collect(Collectors.toList());
            if (params.size() > 0) {
                this.put("params", params);
            }
        }
    }

    private static class ParameterWrapper extends HashMap {

        public ParameterWrapper(Parameter parameter) {
            this.put("name", parameter.getName());
            this.put("type", parameter.getType().getSimpleName());
        }
    }
}
