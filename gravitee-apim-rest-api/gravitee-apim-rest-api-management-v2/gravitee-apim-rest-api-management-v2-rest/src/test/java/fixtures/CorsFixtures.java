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
package fixtures;

import io.gravitee.rest.api.management.v2.rest.model.Cors;
import java.util.Set;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@SuppressWarnings("ALL")
public class CorsFixtures {

    private CorsFixtures() {}

    private static final Cors.CorsBuilder BASE_CORS = Cors.builder()
        .allowCredentials(true)
        .allowHeaders(Set.of("header1", "header2"))
        .allowMethods(Set.of("method1", "method2"))
        .allowOrigin(Set.of("origin1", "origin2"))
        .enabled(true)
        .exposeHeaders(Set.of("exposeHeader1", "exposeHeader2"))
        .maxAge(10)
        .runPolicies(true);

    public static Cors aCors() {
        return BASE_CORS.build();
    }

    public static io.gravitee.definition.model.Cors aModelCors() {
        return CorsModelFixtures.aModelCors();
    }
}
