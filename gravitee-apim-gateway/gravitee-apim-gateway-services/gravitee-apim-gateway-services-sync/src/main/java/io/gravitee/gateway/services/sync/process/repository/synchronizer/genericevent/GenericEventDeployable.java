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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.genericevent;

import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.services.sync.process.common.model.Deployable;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;

/**
 * @param genericEvent the whole event to deploy; on undeploy, only its environment, kind and id
 */
public record GenericEventDeployable(String id, SyncAction syncAction, GenericEvent genericEvent) implements Deployable {}
