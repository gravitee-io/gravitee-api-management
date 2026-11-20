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
package io.gravitee.rest.api.service.notification;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.template.TemplateProcessorException;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import io.gravitee.rest.api.model.UserEntity;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NotificationParamsBuilderTest {

    private static final String PASSWORD_HASH = "$2a$10$hashed-password";

    private final FreemarkerTemplateProcessor templateProcessor = new FreemarkerTemplateProcessor();

    @Test
    void should_not_expose_user_password_to_templates() throws TemplateProcessorException {
        var params = new NotificationParamsBuilder().user(aUser()).build();

        assertThat(templateProcessor.processInlineTemplate("${(user.password)!'not-exposed'}", params)).isEqualTo("not-exposed");
        assertThat(templateProcessor.processInlineTemplate("${user}", params)).doesNotContain(PASSWORD_HASH);
    }

    @Test
    void should_expose_user_properties_used_by_shipped_templates() throws TemplateProcessorException {
        var params = new NotificationParamsBuilder().user(aUser()).build();

        assertThat(
            templateProcessor.processInlineTemplate(
                "${user.displayName}|${user.firstname}|${user.lastname}|${user.email}|${user.status}|${username}",
                params
            )
        ).isEqualTo("Jane Doe|Jane|Doe|jane.doe@gravitee.io|ACTIVE|Jane Doe");
    }

    private static UserEntity aUser() {
        return UserEntity.builder()
            .id("user-id")
            .firstname("Jane")
            .lastname("Doe")
            .email("jane.doe@gravitee.io")
            .status("ACTIVE")
            .password(PASSWORD_HASH)
            .customFields(Map.of("company", "Gravitee"))
            .build();
    }
}
