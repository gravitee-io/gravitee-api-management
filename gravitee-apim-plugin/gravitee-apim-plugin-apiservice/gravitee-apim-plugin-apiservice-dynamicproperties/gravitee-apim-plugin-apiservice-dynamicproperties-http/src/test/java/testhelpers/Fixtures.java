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
package testhelpers;

import static io.gravitee.apim.plugin.apiservice.dynamicproperties.http.HttpDynamicPropertiesService.HTTP_DYNAMIC_PROPERTIES_TYPE;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.plugin.apiservice.dynamicproperties.http.HttpDynamicPropertiesServiceConfiguration;
import io.gravitee.definition.model.v4.AbstractApi;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.nativeapi.NativeApiServices;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.node.api.configuration.Configuration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Fixtures {

    public static final String MY_API = "my-api";

    public static Api apiWithDynamicPropertiesEnabled() {
        return Api.builder()
            .id(MY_API)
            .services(
                ApiServices.builder().dynamicProperty(Service.builder().enabled(true).type(HTTP_DYNAMIC_PROPERTIES_TYPE).build()).build()
            )
            .build();
    }

    public static NativeApi nativeApiWithDynamicPropertiesEnabled() {
        return NativeApi.builder()
            .id(MY_API)
            .services(
                NativeApiServices.builder()
                    .dynamicProperty(Service.builder().enabled(true).type(HTTP_DYNAMIC_PROPERTIES_TYPE).build())
                    .build()
            )
            .build();
    }

    @SneakyThrows
    public static void configureDynamicPropertiesForApi(
        HttpDynamicPropertiesServiceConfiguration configuration,
        AbstractApi api,
        ObjectMapper objectMapper
    ) {
        if (api instanceof Api asHttpApi) {
            asHttpApi.getServices().getDynamicProperty().setConfiguration(objectMapper.writeValueAsString(configuration));
        } else if (api instanceof NativeApi asNativeApi) {
            asNativeApi.getServices().getDynamicProperty().setConfiguration(objectMapper.writeValueAsString(configuration));
        }
    }

    public static Configuration emptyNodeConfiguration() {
        return new Configuration() {
            @Override
            public boolean containsProperty(String s) {
                return false;
            }

            @Override
            public String getProperty(String s) {
                return null;
            }

            @Override
            public String getProperty(String s, String s1) {
                return null;
            }

            @Override
            public <T> T getProperty(String s, Class<T> aClass) {
                return null;
            }

            @Override
            public <T> T getProperty(String s, Class<T> aClass, T t) {
                return null;
            }
        };
    }

    public static String backendResponse() {
        return """
        {
             "props": {
                  "key1": "initial val 1",
                  "key2": "initial val 2"
             }
        }
        """;
    }

    @SneakyThrows
    public static String backendResponseForProperties(List<BackendProperty> returnedProperties, ObjectMapper objectMapper) {
        // Convert list into a LinkedHashMap to keep the insertion order
        final Map<String, String> collect = returnedProperties
            .stream()
            .collect(Collectors.toMap(BackendProperty::key, BackendProperty::value, (oldVal, newVal) -> newVal, LinkedHashMap::new));
        final String propertiesAsJson = new ObjectMapper().writeValueAsString(collect);
        return "{\n" + "     \"props\": " + propertiesAsJson + "}\n";
    }

    public record BackendProperty(String key, String value) {}
}
