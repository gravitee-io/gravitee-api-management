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
package io.gravitee.gateway.handlers.api.manager.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.api.genericevent.GenericEventSubscriber;
import io.gravitee.gateway.handlers.api.manager.ApprovalStateManager;
import lombok.CustomLog;

/**
 * Feeds the {@link ApprovalStateManager} from the {@code approval.state} generic events. A state is deployed as soon as
 * its event arrives, which wakes the calls waiting for it, and forgotten when its event is removed or expires. A copy
 * that cannot be read, whatever the failure, makes the gateway forget the state: the call it belongs to never runs on
 * it, and waits or asks again.
 */
@CustomLog
public class ApprovalStateEventSubscriber implements GenericEventSubscriber {

    private final ApprovalStateManager manager;
    private final ApprovalStateReader reader;

    public ApprovalStateEventSubscriber(ApprovalStateManager manager, ObjectMapper objectMapper) {
        this.manager = manager;
        this.reader = new ApprovalStateReader(objectMapper);
    }

    @Override
    public void onUpsert(GenericEvent event) {
        try {
            manager.deploy(reader.read(event));
        } catch (UnreadableApprovalEventException e) {
            // Without its cause: a parsing error quotes the values of the payload.
            log.error("Unable to read approval state [{}] of environment [{}]: {}", event.id(), event.environmentId(), e.getMessage());
            forgetHeldCopy(event);
        } catch (RuntimeException e) {
            log.error("Unable to read approval state [{}] of environment [{}]", event.id(), event.environmentId(), e);
            forgetHeldCopy(event);
        }
    }

    @Override
    public void onRemove(GenericEvent event) {
        manager.undeploy(event.id());
    }

    /** The copy held may be the one the unreadable copy ends, such as a grant revoked early: kept, it would still grant. */
    private void forgetHeldCopy(GenericEvent event) {
        manager.undeploy(event.id());
    }
}
