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
package io.gravitee.apim.infra.json.jackson.module;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

/**
 * Ensures every definition model enum with a {@code @JsonValue} label that differs from
 * the enum constant name has a serializer in {@link GraviteeDefinitionJacksonModule}.
 *
 * <p>If this test fails, a new enum was added with a mismatched {@code @JsonValue} label but
 * no serializer. Add one to prevent case mismatch during API promotion.</p>
 */
class GraviteeDefinitionJacksonModuleTest {

    @Test
    void all_definition_enums_with_mismatched_json_value_should_have_a_serializer() {
        var mapper = new ObjectMapper();
        mapper.registerModule(new GraviteeDefinitionJacksonModule());

        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Enum.class));

        Set<String> uncovered = scanner
            .findCandidateComponents("io.gravitee.definition.model")
            .stream()
            .map(BeanDefinition::getBeanClassName)
            .map(GraviteeDefinitionJacksonModuleTest::loadClass)
            .filter(type -> type != null && type.isEnum())
            .filter(GraviteeDefinitionJacksonModuleTest::hasMismatchedJsonValueLabel)
            .filter(type -> !hasCustomSerializer(mapper, type))
            .map(Class::getName)
            .collect(Collectors.toSet());

        assertThat(uncovered)
            .as(
                "Definition model enums with @JsonValue labels that differ from enum constant names " +
                    "must have a serializer in GraviteeDefinitionJacksonModule to prevent promotion " +
                    "deserialization failures. Add a serializer for each listed enum."
            )
            .isEmpty();
    }

    private static Class<?> loadClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static boolean hasCustomSerializer(ObjectMapper mapper, Class<?> type) {
        try {
            var provider = mapper.getSerializerProviderInstance();
            var serializer = provider.findTypedValueSerializer(type, true, null);
            return serializer != null && serializer.getClass().getEnclosingClass() == GraviteeDefinitionJacksonModule.class;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean hasMismatchedJsonValueLabel(Class<?> enumType) {
        for (Field field : enumType.getDeclaredFields()) {
            if (field.isAnnotationPresent(JsonValue.class) && field.getType() == String.class) {
                for (Object constant : enumType.getEnumConstants()) {
                    try {
                        field.setAccessible(true);
                        String label = (String) field.get(constant);
                        if (label != null && !label.equals(((Enum<?>) constant).name())) {
                            return true;
                        }
                    } catch (IllegalAccessException e) {
                        // skip
                    }
                }
            }
        }
        for (java.lang.reflect.Method method : enumType.getDeclaredMethods()) {
            if (method.isAnnotationPresent(JsonValue.class) && method.getReturnType() == String.class && method.getParameterCount() == 0) {
                for (Object constant : enumType.getEnumConstants()) {
                    try {
                        String label = (String) method.invoke(constant);
                        if (label != null && !label.equals(((Enum<?>) constant).name())) {
                            return true;
                        }
                    } catch (Exception e) {
                        // skip
                    }
                }
            }
        }
        return false;
    }
}
