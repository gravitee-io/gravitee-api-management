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
package io.gravitee.apim.plugin.gamma.api.automation;

/**
 * Resolves the HRID of a resource of the same module to its internal id, so a module can resolve
 * cross-references carried in a spec (a server pointing at its source, for instance) without owning
 * the HRID-to-id derivation, which stays with the Automation API.
 */
@FunctionalInterface
public interface HridResolver {
    /**
     * @param kindPath the collection path of the referenced kind, e.g. {@code catalog/sources}
     * @param hrid the referenced resource's HRID
     * @return the deterministic internal id the referenced resource has, or would have, in this environment
     */
    String idOf(String kindPath, String hrid);
}
