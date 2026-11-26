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
package io.gravitee.apim.core.credential.service_provider;

import java.util.List;

/**
 * Implemented by each Gamma module whose objects use credentials, so a credential in use cannot be deleted. The module's
 * bean reaches the APIM context when its plugin loads.
 */
public interface CredentialConsumerPort {
    /** The id of the module that answers, as its consumers show it, e.g. {@code aim}. */
    String module();

    /** Every object of this module, in this environment, that references the credential; empty when none does. */
    List<Consumer> consumersOf(String environmentId, String credentialId);

    /**
     * Called after the references or the configuration of a credential changed, for a module that copies them into what it deploys.
     *
     * @return the objects that kept the former settings; empty when every object took the change
     */
    default List<Warning> changed(Change change) {
        return List.of();
    }

    record Consumer(String type, String id, String name) {}

    record Change(String organizationId, String environmentId, String credentialId, String userId) {}

    record Warning(String type, String id, String name, String message) {}
}
