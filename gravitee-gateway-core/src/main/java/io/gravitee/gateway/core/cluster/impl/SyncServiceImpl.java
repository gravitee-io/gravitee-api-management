/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.core.cluster.impl;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.gateway.core.cluster.SyncService;

/**
 * Created by david on 01/08/15.
 */
public class SyncServiceImpl implements SyncService {

    @Override
    public Lifecycle.State lifecycleState() {
        return null;
    }

    @Override
    public Object start() throws Exception {
        return null;
    }

    @Override
    public Object stop() throws Exception {
        return null;
    }
}
