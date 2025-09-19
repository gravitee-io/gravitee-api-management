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
package io.gravitee.apim.infra.domain_service.documentation;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.documentation.exception.InvalidPageContentException;
import io.gravitee.apim.core.documentation.model.ApiFreemarkerTemplate;
import io.gravitee.apim.core.documentation.model.PrimaryOwnerApiTemplateData;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FreemarkerTemplateResolverTest {

    FreemarkerTemplateResolver resolver = new FreemarkerTemplateResolver();

    private static Locale defaultLocale;

    @BeforeAll
    static void beforeAll() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @AfterAll
    static void afterAll() {
        Locale.setDefault(defaultLocale);
    }

    @Test
    void should_resolve_valid_template() {
        assertThat(resolver.resolveTemplate("This is a valid template", Map.of())).isEqualTo("This is a valid template");
    }

    @Test
    void should_resolve_valid_template_with_params() {
        assertThat(resolver.resolveTemplate("This is a valid template with ${bop} injection", Map.of("bop", "simple"))).isEqualTo(
            "This is a valid template with simple injection"
        );
    }

    @Test
    void should_resolve_valid_template_with_complex_params() {
        var coreApi = ApiFreemarkerTemplate.builder().id("id").name("api-name").version("1.0").updatedAt(new Date(0)).build();
        Locale current = Locale.getDefault();
        Locale.setDefault(new Locale("en", "US"));
        assertThat(
            resolver.resolveTemplate(
                "Documentation for ${api.name} ${api.version} (${api.id}) ${api.updatedAt?datetime}",
                Map.of("api", coreApi)
            )
        ).startsWith("Documentation for api-name 1.0 (id) Jan 1, 1970, ");
        Locale.setDefault(current);
    }

    @Test
    void should_resolve_valid_v2_context_path() {
        var virtualHost = new VirtualHost();
        virtualHost.setPath("my-path");

        var virtualHosts = new ArrayList<VirtualHost>();
        virtualHosts.add(virtualHost);

        var proxy = new Proxy();
        proxy.setVirtualHosts(virtualHosts);

        var api = ApiFreemarkerTemplate.builder().id("id").definitionVersion(DefinitionVersion.V2).proxy(proxy).build();

        assertThat(resolver.resolveTemplate("Call me at ${api.proxy.contextPath}", Map.of("api", api))).isEqualTo("Call me at my-path");
    }

    @Test
    void should_throw_exception_if_template_accesses_unknown_data() {
        var throwable = catchThrowable(() ->
            resolver.resolveTemplate("Documentation for ${api.name} ${api.version} (${api.id})", Map.of("api", Api.builder().build()))
        );
        assertThat(throwable).isInstanceOf(InvalidPageContentException.class);
        assertThat(throwable.getCause().getMessage()).contains("api");
    }

    @Test
    void should_throw_exception_if_template_accesses_unknown_property() {
        var api = ApiFreemarkerTemplate.builder().id("id").name("api-name").version("1.0").build();

        var throwable = catchThrowable(() ->
            resolver.resolveTemplate("Documentation for ${api.name} ${api.vvv} (${api.id})", Map.of("api", api))
        );
        assertThat(throwable).isInstanceOf(InvalidPageContentException.class);
        assertThat(throwable.getCause().getMessage()).contains("api.vvv");
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    void should_render_api_fields(final String fieldName, final String output) {
        var po = PrimaryOwnerApiTemplateData.from(
            new PrimaryOwnerEntity("id", "po email", "po display name", PrimaryOwnerEntity.Type.USER)
        );

        var api = ApiFreemarkerTemplate.builder()
            .id("api-id")
            .name("api-name")
            .description("api description")
            .version("v1000")
            .picture("a lovely picture")
            .state(Api.LifecycleState.STARTED)
            .visibility(Api.Visibility.PUBLIC)
            .tags(Set.of("first tag"))
            .metadata(Map.of("meta", "data"))
            .primaryOwner(po)
            .build();

        assertThat(resolver.resolveTemplate("This is my " + fieldName + ": ${api." + fieldName + "}", Map.of("api", api))).isEqualTo(
            "This is my " + fieldName + ": " + output
        );
    }

    public static Stream<Arguments> provideParameters() {
        return Stream.of(
            Arguments.of("id", "api-id"),
            Arguments.of("name", "api-name"),
            Arguments.of("description", "api description"),
            Arguments.of("version", "v1000"),
            Arguments.of("metadata['meta']", "data"),
            Arguments.of("picture", "a lovely picture"),
            Arguments.of("state", "STARTED"),
            Arguments.of("visibility", "PUBLIC"),
            Arguments.of("tags[0]", "first tag"),
            Arguments.of("primaryOwner.displayName", "po display name"),
            Arguments.of("primaryOwner.email", "po email")
        );
    }
}
