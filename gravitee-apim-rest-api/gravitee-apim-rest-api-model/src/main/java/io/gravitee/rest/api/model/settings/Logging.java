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
package io.gravitee.rest.api.model.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.rest.api.model.annotations.ParameterKey;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.settings.logging.MessageSampling;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class Logging {

    @ParameterKey(Key.LOGGING_DEFAULT_MAX_DURATION)
    private Long maxDurationMillis;

    @Builder.Default
    private Audit audit = new Audit();

    @Builder.Default
    private User user = new User();

    @Valid
    @Builder.Default
    private MessageSampling messageSampling = new MessageSampling();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class Audit {

        @ParameterKey(Key.LOGGING_AUDIT_ENABLED)
        private Boolean enabled;

        @Builder.Default
        private AuditTrail trail = new AuditTrail();

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder(toBuilder = true)
        public static class AuditTrail {

            @ParameterKey(Key.LOGGING_AUDIT_TRAIL_ENABLED)
            private Boolean enabled;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class User {

        @ParameterKey(Key.LOGGING_USER_DISPLAYED)
        private Boolean displayed;
    }
}
