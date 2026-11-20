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
package io.gravitee.rest.api.service.builder;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.rest.api.service.builder.EmailNotificationBuilder.EmailTemplate;
import io.gravitee.rest.api.service.notification.HitlHook;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EmailNotificationBuilderTest {

    @ParameterizedTest
    @EnumSource(HitlHook.class)
    void should_resolve_each_hitl_hook_to_its_default_email_template(HitlHook hook) {
        EmailTemplate emailTemplate = EmailTemplate.fromHook(hook);

        assertThat(emailTemplate).isNotNull();
        assertThat(emailTemplate.getLinkedHook()).isEqualTo(hook);
        assertThat(emailTemplate.getHtmlTemplate()).endsWith(".html");
        assertThat(emailTemplate.getSubject()).isNotBlank();
    }

    @Test
    void should_give_each_hitl_hook_its_own_html_template() {
        long distinctHtmlTemplates = Arrays.stream(HitlHook.values())
            .map(EmailTemplate::fromHook)
            .map(EmailTemplate::getHtmlTemplate)
            .distinct()
            .count();

        assertThat(distinctHtmlTemplates).isEqualTo(HitlHook.values().length);
    }
}
