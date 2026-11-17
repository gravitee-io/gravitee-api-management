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
package io.gravitee.gamma.rest.core.guardian.domain_service;

/**
 * What a path does with the configuration a plugin accepts when it is not the one it was given.
 *
 * <p>The schema validation corrects a document before accepting it — it drops null entries, injects
 * the default of a missing required property, prunes what a closed schema forbids — so what a policy
 * or a resource plugin accepts is not always what was submitted.
 */
enum Correction {
    /** Store it: on a write path, what is stored has to be what the plugin validated. */
    STORE,
    /**
     * Refuse the step or the resource: a path that publishes or restores a stored definition may not
     * rewrite it, and publishing a configuration the plugin was never validated with would publish a
     * revision no gateway can start.
     */
    REFUSE,
}
