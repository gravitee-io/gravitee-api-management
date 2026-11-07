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
package io.gravitee.gamma.module.platform.core.secret.inmemory;

import io.gravitee.gamma.module.platform.core.secret.model.Secret;
import io.gravitee.gamma.module.platform.core.secret.model.SecretActor;
import io.gravitee.gamma.module.platform.core.secret.model.SecretAuditEvent;
import io.gravitee.gamma.module.platform.core.secret.port.service_provider.SecretAuditLog;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SecretAuditLogInMemory implements SecretAuditLog {

    public record Entry(SecretAuditEvent event, Secret before, Secret after, SecretActor actor, Instant at) {}

    private final List<Entry> entries = new ArrayList<>();

    @Override
    public void record(SecretAuditEvent event, Secret before, Secret after, SecretActor actor, Instant at) {
        entries.add(new Entry(event, before, after, actor, at));
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    public Entry last() {
        return entries.getLast();
    }
}
