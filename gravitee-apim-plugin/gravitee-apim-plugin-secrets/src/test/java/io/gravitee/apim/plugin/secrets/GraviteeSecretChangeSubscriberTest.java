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
package io.gravitee.apim.plugin.secrets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.secrets.api.event.SecretInvalidationEvent;
import io.gravitee.secrets.api.event.SecretInvalidationEventType;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GraviteeSecretChangeSubscriberTest {

    private final EventManager eventManager = mock(EventManager.class);
    private final GraviteeSecretChangeSubscriber subscriber = new GraviteeSecretChangeSubscriber(eventManager);

    @Test
    void should_ask_the_secrets_service_to_resolve_again_the_secret_that_changed() {
        subscriber.onUpsert(changed("env-1", "db-password"));

        verify(eventManager).publishEvent(
            SecretInvalidationEventType.INVALIDATE,
            new SecretInvalidationEvent("env-1", GraviteeSecretProvider.ID, "db-password")
        );
    }

    @Test
    void should_ignore_a_change_event_that_expired() {
        subscriber.onRemove(changed("env-1", "db-password"));

        verifyNoInteractions(eventManager);
    }

    private static GenericEvent changed(String environmentId, String key) {
        return new GenericEvent("secret.changed", key, environmentId, "org-1", null, Instant.now(), "{\"key\":\"" + key + "\"}");
    }
}
