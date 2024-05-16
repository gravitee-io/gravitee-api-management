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
package io.gravitee.repository.common.query;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Aurelien PACAUD (aurelien.pacaud at graviteesource.com)
 * @author GraviteeSource Team
 */
@AllArgsConstructor
@Getter
public class QueryContext {

    private static final String ORG_ID_PLACEHOLDER_KEY = "orgId";
    private static final String ENV_ID_PLACEHOLDER_KEY = "envId";

    private final String orgId;
    private final String envId;

    public Map<String, String> placeholder() {
        return Map.of(ORG_ID_PLACEHOLDER_KEY, orgId, ENV_ID_PLACEHOLDER_KEY, envId);
    }
}
