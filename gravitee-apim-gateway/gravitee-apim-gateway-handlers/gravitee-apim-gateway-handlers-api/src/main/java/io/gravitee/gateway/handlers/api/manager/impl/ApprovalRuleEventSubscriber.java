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
import io.gravitee.gateway.handlers.api.manager.ApprovalRuleManager;
import lombok.CustomLog;

/**
 * Feeds the {@link ApprovalRuleManager} from the {@code approval.rule} generic events: a rule is staged when its event
 * is upserted or removed, and the whole batch is applied at the commit. A rule that cannot be read is never skipped
 * silently, since it may cover any call: the manager refuses the calls of its environment until a readable copy arrives.
 */
@CustomLog
public class ApprovalRuleEventSubscriber implements GenericEventSubscriber {

    private final ApprovalRuleManager manager;
    private final ApprovalRuleReader reader;

    public ApprovalRuleEventSubscriber(ApprovalRuleManager manager, ObjectMapper objectMapper) {
        this.manager = manager;
        this.reader = new ApprovalRuleReader(objectMapper);
    }

    @Override
    public void onUpsert(GenericEvent event) {
        try {
            manager.deploy(reader.read(event));
        } catch (UnreadableApprovalEventException e) {
            // Without its cause: a parsing error quotes the values of the payload.
            log.error("Unable to read approval rule [{}] of environment [{}]: {}", event.id(), event.environmentId(), e.getMessage());
            manager.markUnreadable(event.environmentId(), event.id());
        } catch (RuntimeException e) {
            // A rule skipped without being marked would leave the calls it covers ungated, whatever the failure.
            log.error("Unable to read approval rule [{}] of environment [{}]", event.id(), event.environmentId(), e);
            manager.markUnreadable(event.environmentId(), event.id());
        }
    }

    @Override
    public void onRemove(GenericEvent event) {
        manager.undeploy(event.environmentId(), event.id());
    }

    @Override
    public void onCommit() {
        manager.commit();
    }
}
