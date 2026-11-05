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
package io.gravitee.gateway.handlers.api.manager;

import io.gravitee.gamma.definition.approval.ApprovalOutcomeCommand;
import io.gravitee.gamma.definition.approval.ApprovalRequestCommand;
import io.reactivex.rxjava3.core.Completable;

/**
 * Tells the control plane about held calls by writing commands for the Management API, the only way a gateway reaches
 * it, including through the bridge. Writes happen off the event loop.
 */
public interface ApprovalCommandWriter {
    Completable send(ApprovalRequestCommand command);

    Completable send(ApprovalOutcomeCommand command);
}
