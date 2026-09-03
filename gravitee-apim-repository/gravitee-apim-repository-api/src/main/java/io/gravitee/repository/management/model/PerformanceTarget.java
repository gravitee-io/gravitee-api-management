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
package io.gravitee.repository.management.model;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Setter
@ToString
public final class PerformanceTarget {

    @EqualsAndHashCode.Include
    private String id;

    private String environmentId;
    private String reference;

    @Builder.Default
    private List<String> apiIds = Collections.emptyList();

    private long windowSeconds;
    private long intervalSeconds;
    private int minSampleSize;

    @Builder.Default
    private List<Rule> rules = Collections.emptyList();

    private Date createdAt;
    private Date updatedAt;

    public record Rule(String metric, String measure, String operator, double threshold, List<String> apiTypes, List<Filter> filters) {}

    public record Filter(String name, String operator, Object value) {}
}
