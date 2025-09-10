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
package io.gravitee.rest.api.model.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNotificationConfigEntityTest {

    @ParameterizedTest
    @CsvSource(value = { "foo,'',''", "'',foo,''", "'',foo,''", "null,'',''", "'',null,''", "'',null,''" }, nullValues = "null")
    void should_fail_to_create_default_notification_settings(String user, String referenceType, String referenceId) {
        assertThatThrownBy(() -> PortalNotificationConfigEntity.newDefaultEmpty(user, referenceType, referenceId, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_create_empty_default_config() {
        PortalNotificationConfigEntity portalNotificationConfigEntity = PortalNotificationConfigEntity.newDefaultEmpty(
            "foo",
            "API",
            "bar",
            "org1"
        );
        assertThat(portalNotificationConfigEntity.isDefaultEmpty()).isTrue();
    }

    @Test
    void should_be_default_empty_config() {
        PortalNotificationConfigEntity empty = new PortalNotificationConfigEntity();
        assertThat(empty.isDefaultEmpty()).isFalse();

        empty.setUser("user");
        empty.setReferenceId("123");
        empty.setReferenceType("API");
        empty.setOrganizationId("org1");
        assertThat(empty.isDefaultEmpty()).isTrue();

        empty.setHooks(Arrays.asList("A", "B", "C"));
        assertThat(empty.isDefaultEmpty()).isFalse();
    }
}
