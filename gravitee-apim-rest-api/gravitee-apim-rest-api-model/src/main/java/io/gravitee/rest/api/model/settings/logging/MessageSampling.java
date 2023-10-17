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
package io.gravitee.rest.api.model.settings.logging;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.rest.api.model.annotations.ParameterKey;
import io.gravitee.rest.api.model.parameters.Key;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@ValidMessageSamplingSettings
@NoArgsConstructor
@AllArgsConstructor
public class MessageSampling {

    @Valid
    @NotNull
    @Builder.Default
    private MessageSampling.Probabilistic probabilistic = Probabilistic.builder().build();

    @Valid
    @NotNull
    @Builder.Default
    private MessageSampling.Count count = Count.builder().build();

    @Valid
    @NotNull
    @Builder.Default
    private MessageSampling.Temporal temporal = Temporal.builder().build();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Probabilistic {

        @DecimalMin("0.01")
        @Builder.Default
        @JsonProperty("default")
        @ParameterKey(Key.LOGGING_MESSAGE_SAMPLING_PROBABILISTIC_DEFAULT)
        private Double defaultValue = 0.01;

        @DecimalMin("0.01")
        @Builder.Default
        @ParameterKey(Key.LOGGING_MESSAGE_SAMPLING_PROBABILISTIC_LIMIT)
        private Double limit = 0.5;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Count {

        @Min(1)
        @Builder.Default
        @JsonProperty("default")
        @ParameterKey(Key.LOGGING_MESSAGE_SAMPLING_COUNT_DEFAULT)
        private Integer defaultValue = 100;

        @Min(1)
        @Builder.Default
        @ParameterKey(Key.LOGGING_MESSAGE_SAMPLING_COUNT_LIMIT)
        private Integer limit = 10;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Temporal {

        @Builder.Default
        @JsonProperty("default")
        @ParameterKey(Key.LOGGING_MESSAGE_SAMPLING_TEMPORAL_DEFAULT)
        private String defaultValue = "PT1S";

        @Builder.Default
        @ParameterKey(Key.LOGGING_MESSAGE_SAMPLING_TEMPORAL_LIMIT)
        private String limit = "PT1S";
    }
}
