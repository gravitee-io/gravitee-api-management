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
package io.gravitee.gateway.handlers.guardian.manager;

import io.gravitee.gateway.handlers.guardian.ReactableGuardian;
import java.util.Collection;

/**
 * Deploys Guardians on the gateway. A Guardian is identified by its id within an environment; the same id can be
 * deployed independently in several environments.
 *
 * @author GraviteeSource Team
 */
public interface GuardianManager {
    /**
     * Deploy a Guardian, or update it when a more recent copy of an already deployed Guardian is given.
     * @param guardian the Guardian to deploy.
     * @return {@code true} when the Guardian has been deployed or updated.
     */
    boolean register(ReactableGuardian guardian);

    void unregister(String guardianId, String environmentId);

    /**
     * Undeploy every copy of this Guardian. An undeploy event with no environment applies to all environments.
     * @param guardianId the id of the Guardian.
     */
    void unregisterAll(String guardianId);

    Collection<ReactableGuardian> guardians();

    ReactableGuardian get(String guardianId, String environmentId);
}
