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
package io.gravitee.apim.core.notification.domain_service;

import static org.assertj.core.api.Assertions.LIST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.AuditInfoFixtures;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.group.domain_service.ValidateGroupsDomainService;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.rest.api.model.notification.NotificationConfigType;
import io.gravitee.rest.api.model.notification.PortalNotificationConfigEntity;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidatePortalNotificationDomainServiceTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String ACTOR_USER_ID = "actor-user-id";

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, ACTOR_USER_ID);

    ValidatePortalNotificationDomainService underTest;

    ValidateGroupsDomainService groupValidator = mock(ValidateGroupsDomainService.class);

    @BeforeEach
    void setUp() {
        underTest = new ValidatePortalNotificationDomainService(groupValidator);
    }

    @Test
    void should_validate_null_notification() {
        var input = new ValidatePortalNotificationDomainService.Input(null, "4.0.0", null, AUDIT_INFO);
        var result = underTest.validateAndSanitize(input);

        verify(groupValidator, never()).validateAndSanitize(any());

        assertThat(result.value()).isPresent();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void should_validate_valid_notification() {
        ValidateGroupsDomainService.Input groupInput = emptyGroupInput();
        when(groupValidator.validateAndSanitize(groupInput)).thenReturn(Validator.Result.ofValue(groupInput));

        PortalNotificationConfigEntity notification = portalNotification(NotificationConfigType.PORTAL);
        var input = new ValidatePortalNotificationDomainService.Input(notification, "4.0.0", null, AUDIT_INFO);

        var result = underTest.validateAndSanitize(input);
        assertThat(result.value()).isPresent();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void should_not_validate_non_portal_notification() {
        ValidateGroupsDomainService.Input groupInput = emptyGroupInput();
        when(groupValidator.validateAndSanitize(groupInput)).thenReturn(Validator.Result.ofValue(groupInput));

        PortalNotificationConfigEntity notification = portalNotification(NotificationConfigType.GENERIC);
        var input = new ValidatePortalNotificationDomainService.Input(notification, "4.0.0", null, AUDIT_INFO);

        var result = underTest.validateAndSanitize(input);

        assertThat(result.value()).isPresent();
        assertThat(result.errors()).isPresent();
        assertThat(result.severe())
            .get()
            .asInstanceOf(LIST)
            .hasSize(1)
            .allMatch(e -> ((Validator.Error) e).getMessage().contains("'PORTAL'"));
    }

    @Test
    void should_not_validate_when_groups_invalid() {
        when(groupValidator.validateAndSanitize(any())).thenReturn(Validator.Result.ofErrors(List.of(Validator.Error.severe("failed!"))));

        PortalNotificationConfigEntity notification = portalNotification(NotificationConfigType.PORTAL);
        var input = new ValidatePortalNotificationDomainService.Input(notification, "4.0.0", null, AUDIT_INFO);

        var result = underTest.validateAndSanitize(input);

        assertThat(result.value()).isPresent();
        assertThat(result.errors()).isPresent();
        assertThat(result.severe()).get().asInstanceOf(LIST).extracting("message").containsExactly("failed!");
    }

    public static Stream<Arguments> notificationGroups() {
        return Stream.of(
            arguments(Set.of(), Set.of("group-1"), true),
            arguments(null, Set.of("group-1"), true),
            arguments(Set.of("group-2"), Set.of("group-1"), true),
            arguments(Set.of("group-2"), Set.of("group-1", "group-2"), true),
            arguments(Set.of(), Set.of(), false),
            arguments(Set.of(), null, false),
            arguments(null, Set.of(), false),
            arguments(null, null, false),
            arguments(Set.of("group-1"), Set.of(), false),
            arguments(Set.of("group-1"), null, false)
        );
    }

    @MethodSource("notificationGroups")
    @ParameterizedTest
    void should_validate_with_allowed_groups(Set<String> allowedGroups, Set<String> consoleNotificationGroup, boolean expectErrors) {
        when(groupValidator.validateAndSanitize(any())).thenReturn(Validator.Result.ofValue(groupInput(consoleNotificationGroup)));

        PortalNotificationConfigEntity notification = portalNotification(NotificationConfigType.PORTAL);
        notification.setGroups(CollectionUtils.stream(consoleNotificationGroup).toList());
        var input = new ValidatePortalNotificationDomainService.Input(notification, "4.0.0", allowedGroups, AUDIT_INFO);

        var result = underTest.validateAndSanitize(input);

        if (expectErrors) {
            assertThat(result.value()).isPresent();
            assertThat(result.errors()).isPresent();
            assertThat(result.severe())
                .get()
                .asInstanceOf(InstanceOfAssertFactories.list(Validator.Error.class))
                .extracting(Validator.Error::getMessage)
                .singleElement()
                .asInstanceOf(STRING)
                .contains("where only those are allowed %s".formatted(allowedGroups));
        } else {
            assertThat(result.value()).isPresent();
            assertThat(result.errors()).isEmpty();
        }
    }

    @Nonnull
    private static PortalNotificationConfigEntity portalNotification(NotificationConfigType type) {
        PortalNotificationConfigEntity notification = new PortalNotificationConfigEntity();
        notification.setConfigType(type);
        return notification;
    }

    private static ValidateGroupsDomainService.Input emptyGroupInput() {
        return groupInput(Set.of());
    }

    private static ValidateGroupsDomainService.Input groupInput(Set<String> groups) {
        return new ValidateGroupsDomainService.Input(ENVIRONMENT_ID, groups, "4.0.0", null, false);
    }
}
