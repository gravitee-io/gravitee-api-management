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
package io.gravitee.apim.plugin.gamma.api;

/**
 * Interface to be implemented by Gamma modules.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface GammaModule {
    /**
     * Get the REST resource class to be registered for this module.
     * The returned class must be annotated with JAX-RS annotations and will be registered under the path "/gamma/modules/{pluginId}".
     *
     * @return the REST resource class to be registered for this module, or null if no REST resource should be registered.
     */
    default Class<?> restResource() {
        return null;
    }
}
