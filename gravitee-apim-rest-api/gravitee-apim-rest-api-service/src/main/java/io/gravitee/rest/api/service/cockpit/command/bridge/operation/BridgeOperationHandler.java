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
package io.gravitee.rest.api.service.cockpit.command.bridge.operation;

import io.gravitee.cockpit.api.command.bridge.BridgeCommand;
import io.gravitee.cockpit.api.command.bridge.BridgeReply;
import io.reactivex.rxjava3.core.Single;

public interface BridgeOperationHandler {
    boolean canHandle(String bridgeOperation);

    Single<BridgeReply> handle(BridgeCommand bridgeCommand);
}
