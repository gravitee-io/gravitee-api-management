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
package io.gravitee.definition.model.v4.analytics.sampling;

import com.google.common.base.Splitter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import lombok.*;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Schema(name = "SamplingV4")
public class Sampling implements Serializable {

    @NotNull
    private SamplingType type;

    @NotEmpty
    @NotNull
    private String value;

    public record WindowedCount(int count, Duration window) {
        public String encode() {
            return count + "/" + window;
        }

        public double ratePerSeconds() {
            return (double) count / window.toSeconds();
        }
    }

    public static WindowedCount parseWindowedCount(@Nonnull String value) {
        List<String> parts = List.of(value.split("/"));
        if (parts.size() != 2) {
            throw new IllegalArgumentException("Invalid windowed count value: " + value + ", must be of the form <count>/<duration>");
        }
        return new WindowedCount(Integer.parseInt(parts.getFirst()), Duration.parse(parts.getLast()));
    }
}
