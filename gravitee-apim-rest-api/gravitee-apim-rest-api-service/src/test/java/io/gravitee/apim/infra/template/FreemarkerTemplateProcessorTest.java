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
package io.gravitee.apim.infra.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.gravitee.apim.core.template.TemplateProcessorException;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.junit.jupiter.api.Test;

class FreemarkerTemplateProcessorTest {

    private final FreemarkerTemplateProcessor cut = new FreemarkerTemplateProcessor();

    @Getter
    @Builder
    @ToString
    public static class TestData {

        private String id;
    }

    @Test
    void should_process_a_simple_template() throws TemplateProcessorException {
        var result = cut.processInlineTemplate("Hello World!", Map.of());
        assertThat(result).isEqualTo("Hello World!");
    }

    @Test
    void should_process_a_template_with_variables() throws TemplateProcessorException {
        var result = cut.processInlineTemplate("Hello ${name}!", Map.of("name", "Gravitee"));
        assertThat(result).isEqualTo("Hello Gravitee!");
    }

    @Test
    void should_throw_a_TemplateProcessorException_for_an_invalid_template() {
        assertThatExceptionOfType(TemplateProcessorException.class)
            .isThrownBy(() -> cut.processInlineTemplate("Hello ${name!", Map.of()))
            .withCauseInstanceOf(freemarker.core.ParseException.class);
    }

    @Test
    void should_throw_a_TemplateProcessorException_when_accessing_restricted_classes() {
        assertThatExceptionOfType(TemplateProcessorException.class)
            .isThrownBy(() -> cut.processInlineTemplate("${'java.lang.System'?new()}", Map.of()))
            .withCauseInstanceOf(freemarker.core._MiscTemplateException.class);
    }

    @Test
    void should_process_a_template_with_field_access() throws TemplateProcessorException {
        var data = TestData.builder().id("api-id").build();
        var result = cut.processInlineTemplate("${api.id}", Map.of("api", data));
        assertThat(result).isEqualTo("api-id");
    }

    @Test
    void should_process_to_string() throws TemplateProcessorException {
        var data = TestData.builder().id("api-id").build();
        var result = cut.processInlineTemplate("${api.toString()}", Map.of("api", data));
        assertThat(result).contains("id=api-id");
    }

    @Test
    void should_not_process_a_template_with_get_class() {
        var data = TestData.builder().id("api-id").build();
        assertThatExceptionOfType(TemplateProcessorException.class).isThrownBy(() ->
            cut.processInlineTemplate("${api.class}", Map.of("api", data))
        );
    }

    @Test
    void should_not_process_a_template_with_restricted_methods() {
        var data = TestData.builder().id("api-id").build();
        assertThatExceptionOfType(TemplateProcessorException.class).isThrownBy(() ->
            cut.processInlineTemplate("${api.class.protectionDomain}", Map.of("api", data))
        );
    }

    @Test
    void should_not_process_a_template_with_restricted_methods_combination() {
        var data = TestData.builder().id("api-id").build();
        assertThatExceptionOfType(TemplateProcessorException.class).isThrownBy(() ->
            cut.processInlineTemplate(
                "${api.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().resolve('/etc/passwd').toURL().openStream()}",
                Map.of("api", data)
            )
        );
    }
}
