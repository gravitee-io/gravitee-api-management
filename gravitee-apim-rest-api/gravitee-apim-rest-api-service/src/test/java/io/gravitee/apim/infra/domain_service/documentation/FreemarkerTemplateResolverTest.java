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

import io.gravitee.apim.core.documentation.exception.InvalidPageContentException;
import io.gravitee.rest.api.model.ApiModel;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FreemarkerTemplateResolverTest {

    FreemarkerTemplateResolver resolver = new FreemarkerTemplateResolver();

    @Test
    void should_resolve_valid_template() {
        assertThat(resolver.resolveTemplate("This is a valid template", Map.of())).isEqualTo("This is a valid template");
    }

    @Test
    void should_resolve_valid_template_with_params() {
        assertThat(resolver.resolveTemplate("This is a valid template with ${bop} injection", Map.of("bop", "simple")))
            .isEqualTo("This is a valid template with simple injection");
    }

    @Test
    void should_resolve_valid_template_with_complex_params() {
        ApiModel api = new ApiModel();
        api.setId("id");
        api.setName("api-name");
        api.setVersion("1.0");

        assertThat(resolver.resolveTemplate("Documentation for ${api.name} ${api.version} (${api.id})", Map.of("api", api)))
            .isEqualTo("Documentation for api-name 1.0 (id)");
    }

    @Test
    void should_throw_exception_if_template_accesses_unknown_data() {
        var throwable = catchThrowable(() -> resolver.resolveTemplate("Documentation for ${api.name} ${api.version} (${api.id})", Map.of())
        );
        assertThat(throwable).isInstanceOf(InvalidPageContentException.class);
        assertThat(throwable.getCause().getMessage()).contains("api");
    }

    @Test
    void should_throw_exception_if_template_accesses_unknown_property() {
        ApiModel api = new ApiModel();
        api.setId("id");
        api.setName("api-name");
        api.setVersion("1.0");

        var throwable = catchThrowable(() ->
            resolver.resolveTemplate("Documentation for ${api.name} ${api.vvv} (${api.id})", Map.of("api", api))
        );
        assertThat(throwable).isInstanceOf(InvalidPageContentException.class);
        assertThat(throwable.getCause().getMessage()).contains("api.vvv");
    }
}
