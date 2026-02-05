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
package io.gravitee.repository.jdbc.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

public class JdbcGenericNotificationConfigRepositoryTest {

    private JdbcGenericNotificationConfigRepository repository = new JdbcGenericNotificationConfigRepository("table_prefix_");
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @SneakyThrows
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        Field field = JdbcAbstractRepository.class.getDeclaredField("jdbcTemplate");
        field.setAccessible(true);
        field.set(repository, jdbcTemplate);
        field.setAccessible(false);
    }

    @Test
    void findAll_shouldLoadHooks() throws Exception {
        GenericNotificationConfig config = GenericNotificationConfig.builder()
            .id("config1")
            .name("Config 1")
            .notifier("email")
            .referenceType(NotificationReferenceType.API)
            .referenceId("api1")
            .organizationId("org1")
            .build();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(config));

        doAnswer(inv -> {
            config.setHooks(List.of("HOOK_1"));
            return null;
        })
            .when(jdbcTemplate)
            .query(contains("generic_notification_config_hooks"), any(RowCallbackHandler.class));

        Set<GenericNotificationConfig> result = repository.findAll();

        assertThat(result).hasSize(1);
        GenericNotificationConfig cfg = result.iterator().next();

        assertThat(cfg.getHooks()).containsExactly("HOOK_1");
    }
}
