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
package io.gravitee.apim.authorization.audit;

import io.gravitee.apim.authorization.api.AuthzAuditEntry;
import io.gravitee.apim.authorization.api.AuthzAuditPort;
import io.gravitee.apim.authorization.api.AuthzAuditReferenceKind;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class RecordingAuthzAuditPort implements AuthzAuditPort {

    private final List<AuthzAuditEntry> entries = new CopyOnWriteArrayList<>();

    @Override
    public void record(AuthzAuditEntry entry) {
        entries.add(entry);
    }

    public List<AuthzAuditEntry> entries() {
        return List.copyOf(entries);
    }

    public List<AuthzAuditEntry> eventsFor(AuthzAuditReferenceKind referenceKind) {
        List<AuthzAuditEntry> filtered = new ArrayList<>();
        for (AuthzAuditEntry e : entries) {
            if (e.referenceKind() == referenceKind) {
                filtered.add(e);
            }
        }
        return filtered;
    }

    public void clear() {
        entries.clear();
    }
}
