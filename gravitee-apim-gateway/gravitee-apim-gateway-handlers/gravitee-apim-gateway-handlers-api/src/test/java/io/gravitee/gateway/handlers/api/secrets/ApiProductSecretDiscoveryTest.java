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
package io.gravitee.gateway.handlers.api.secrets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.secrets.api.event.SecretDiscoveryEvent;
import io.gravitee.secrets.api.event.SecretDiscoveryEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductSecretDiscoveryTest {

    @Mock
    private EventManager eventManager;

    private ApiProductSecretDiscovery underTest;

    @BeforeEach
    void setUp() {
        underTest = new ApiProductSecretDiscovery(eventManager);
    }

    @Test
    void should_discover_the_secrets_of_a_version_when_it_is_first_bound() {
        ReactableApiProduct version = product();

        underTest.bind(version);

        SecretDiscoveryEvent discovered = published(SecretDiscoveryEventType.DISCOVER);
        assertThat(discovered.envId()).isEqualTo("env-1");
        assertThat(discovered.definition()).isSameAs(version);
        assertThat(discovered.metadata().revision()).isNotBlank();
    }

    @Test
    void should_discover_a_version_once_however_many_times_it_is_bound() {
        ReactableApiProduct version = product();

        underTest.bind(version);
        underTest.bind(version);

        verify(eventManager, times(1)).publishEvent(any(SecretDiscoveryEventType.class), any(SecretDiscoveryEvent.class));
    }

    @Test
    void should_revoke_the_secrets_of_a_version_only_when_its_last_binding_is_released() {
        ReactableApiProduct version = product();
        underTest.bind(version);
        underTest.bind(version);
        String revision = published(SecretDiscoveryEventType.DISCOVER).metadata().revision();

        underTest.release(version);
        verify(eventManager, never()).publishEvent(any(SecretDiscoveryEventType.class), any(SecretDiscoveryEvent.class));

        underTest.release(version);
        SecretDiscoveryEvent revoked = published(SecretDiscoveryEventType.REVOKE);
        assertThat(revoked.definition()).isSameAs(version);
        assertThat(revoked.metadata().revision()).isEqualTo(revision);
    }

    @Test
    void should_give_each_version_its_own_revision_so_revoking_one_keeps_the_other() {
        ReactableApiProduct first = product();
        ReactableApiProduct second = product();

        underTest.bind(first);
        String firstRevision = published(SecretDiscoveryEventType.DISCOVER).metadata().revision();
        underTest.bind(second);
        String secondRevision = published(SecretDiscoveryEventType.DISCOVER).metadata().revision();

        assertThat(secondRevision).isNotEqualTo(firstRevision);
    }

    @Test
    void should_revoke_what_a_failed_discovery_granted_and_keep_no_binding_for_it() {
        ReactableApiProduct version = product();
        RuntimeException failure = new IllegalStateException("malformed secret reference");
        doThrow(failure).when(eventManager).publishEvent(eq(SecretDiscoveryEventType.DISCOVER), any(SecretDiscoveryEvent.class));

        assertThatThrownBy(() -> underTest.bind(version)).isSameAs(failure);

        ArgumentCaptor<SecretDiscoveryEvent> events = ArgumentCaptor.forClass(SecretDiscoveryEvent.class);
        verify(eventManager).publishEvent(eq(SecretDiscoveryEventType.DISCOVER), events.capture());
        verify(eventManager).publishEvent(eq(SecretDiscoveryEventType.REVOKE), events.capture());
        assertThat(events.getAllValues().get(1).metadata().revision()).isEqualTo(events.getAllValues().get(0).metadata().revision());

        clearInvocations(eventManager);
        doNothing().when(eventManager).publishEvent(eq(SecretDiscoveryEventType.DISCOVER), any(SecretDiscoveryEvent.class));
        underTest.release(version);
        verifyNoInteractions(eventManager);
        underTest.bind(version);
        published(SecretDiscoveryEventType.DISCOVER);
    }

    @Test
    void should_still_fail_with_the_discovery_failure_when_its_revoke_fails_too() {
        ReactableApiProduct version = product();
        RuntimeException failure = new IllegalStateException("malformed secret reference");
        doThrow(failure).when(eventManager).publishEvent(eq(SecretDiscoveryEventType.DISCOVER), any(SecretDiscoveryEvent.class));
        doThrow(new IllegalStateException("revoke failed"))
            .when(eventManager)
            .publishEvent(eq(SecretDiscoveryEventType.REVOKE), any(SecretDiscoveryEvent.class));

        assertThatThrownBy(() -> underTest.bind(version)).isSameAs(failure);
    }

    @Test
    void should_ignore_the_release_of_a_version_never_bound() {
        underTest.release(product());

        verifyNoInteractions(eventManager);
    }

    @Test
    void should_ignore_a_missing_version() {
        underTest.bind(null);
        underTest.release(null);

        verifyNoInteractions(eventManager);
    }

    private SecretDiscoveryEvent published(SecretDiscoveryEventType type) {
        ArgumentCaptor<SecretDiscoveryEvent> event = ArgumentCaptor.forClass(SecretDiscoveryEvent.class);
        verify(eventManager, atLeastOnce()).publishEvent(eq(type), event.capture());
        clearInvocations(eventManager);
        return event.getValue();
    }

    private static ReactableApiProduct product() {
        return ReactableApiProduct.builder().id("product-1").environmentId("env-1").build();
    }
}
