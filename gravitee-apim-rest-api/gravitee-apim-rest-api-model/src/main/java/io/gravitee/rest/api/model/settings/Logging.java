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
import lombok.Getter;
import lombok.Setter;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Logging {

    @ParameterKey(Key.LOGGING_DEFAULT_MAX_DURATION)
    private Long maxDurationMillis;

    private Audit audit = new Audit();
    private User user = new User();

    @Valid
    private MessageSampling messageSampling = new MessageSampling();

    @Getter
    @Setter
    public static class Audit {

        @ParameterKey(Key.LOGGING_AUDIT_ENABLED)
        private Boolean enabled;

        private AuditTrail trail = new AuditTrail();

        @Getter
        @Setter
        public static class AuditTrail {

            @ParameterKey(Key.LOGGING_AUDIT_TRAIL_ENABLED)
            private Boolean enabled;
        }
    }

    @Getter
    @Setter
    public static class User {

        @ParameterKey(Key.LOGGING_USER_DISPLAYED)
        private Boolean displayed;
    }

    @Getter
    @Setter
    public static class MessageSamplingYouhou {

        private Count count = new Count();
        private Probabilistic probabilistic = new Probabilistic();
        private Temporal temporal = new Temporal();

        @Getter
        @Setter
        public static class Count {

            @JsonProperty("default")
            @ParameterKey(Key.LOGGING_MESSAGE_SAMPLING_COUNT_DEFAULT)
            private Integer defaultValue;

            @ParameterKey(Key.LOGGING_MESSAGE_SAMPLING_COUNT_LIMIT)
            private Integer limit;
        }

        @Getter
        @Setter
        public static class Probabilistic {

            @JsonProperty("default")
            @ParameterKey(Key.LOGGING_MESSAGE_SAMPLING_PROBABILISTIC_DEFAULT)
            private Double defaultValue;

            @ParameterKey(Key.LOGGING_MESSAGE_SAMPLING_PROBABILISTIC_LIMIT)
            private Double limit;
        }

        @Getter
        @Setter
        public static class Temporal {

            @JsonProperty("default")
            @ParameterKey(Key.LOGGING_MESSAGE_SAMPLING_TEMPORAL_DEFAULT)
            private String defaultValue;

            @ParameterKey(Key.LOGGING_MESSAGE_SAMPLING_TEMPORAL_LIMIT)
            private String limit;
        }
    }
}
