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
package io.gravitee.gamma.rest.architecture;

/**
 * Shared package constants for the ArchUnit rule classes enforcing the gamma-rest-api AGENTS.md
 * conventions (§1 Layout, §3 Use cases, §5 Core independence, §9 REST conventions).
 *
 * <p>{@code RESOURCES_PACKAGE} maps to the existing {@code resources/} (plural) folder where all
 * JAX-RS classes live — host routing resources at the root and per-domain global resources nested
 * under {@code resources/<domain>/}. See the AGENTS layout section for the rationale.
 */
final class AbstractGammaArchitectureTest {

    private AbstractGammaArchitectureTest() {}

    static final String BASE_PACKAGE = "io.gravitee.gamma.rest";
    static final String CORE_PACKAGE = BASE_PACKAGE + ".core..";
    static final String INFRA_PACKAGE = BASE_PACKAGE + ".infra..";
    static final String RESOURCES_PACKAGE = BASE_PACKAGE + ".resources..";
}
