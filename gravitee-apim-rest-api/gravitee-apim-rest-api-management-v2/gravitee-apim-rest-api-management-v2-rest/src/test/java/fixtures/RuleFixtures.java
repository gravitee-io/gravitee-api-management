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
package fixtures;

import io.gravitee.definition.model.Policy;
import io.gravitee.rest.api.management.v2.rest.model.HttpMethod;
import io.gravitee.rest.api.management.v2.rest.model.Rule;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@SuppressWarnings("ALL")
public class RuleFixtures {

    private static final io.gravitee.definition.model.Rule.RuleBuilder BASE_MODEL_RULE = io.gravitee.definition.model.Rule
        .builder()
        .description("description")
        .enabled(true)
        .methods(Set.of(io.gravitee.common.http.HttpMethod.GET, io.gravitee.common.http.HttpMethod.POST))
        .policy(Policy.builder().name("policy-name").configuration("{ }").build());

    private static final Rule.RuleBuilder BASE_RULE = Rule
        .builder()
        .description("description")
        .enabled(true)
        .methods(List.of(HttpMethod.GET, HttpMethod.POST))
        .operation("policy-name")
        .configuration(new LinkedHashMap<>());

    public static io.gravitee.definition.model.Rule oneModelRule() {
        return BASE_MODEL_RULE.build();
    }

    public static Rule oneRule() {
        return BASE_RULE.build();
    }
}
