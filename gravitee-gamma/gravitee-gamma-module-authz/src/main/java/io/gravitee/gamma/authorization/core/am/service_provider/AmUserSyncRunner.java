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
package io.gravitee.gamma.authorization.core.am.service_provider;

import io.gravitee.apim.core.async_job.model.AsyncJob;
import io.gravitee.apim.plugin.gamma.api.identity.AmConnection;
import io.gravitee.gamma.authorization.api.AuthzCallerContext;

/**
 * Port that runs a created (PENDING) AM user-sync {@link AsyncJob} on a background worker and
 * transitions it to SUCCESS/ERROR when done. The infra implementation owns the worker pool; the
 * sync work itself stays in the core use case it invokes.
 */
public interface AmUserSyncRunner {
    void runAsync(AsyncJob job, AuthzCallerContext caller, AmConnection connection);
}
