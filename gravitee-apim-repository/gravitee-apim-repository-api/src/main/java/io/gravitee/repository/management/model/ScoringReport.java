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
import lombok.Singular;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Setter
@ToString
public final class ScoringReport {

    @EqualsAndHashCode.Include
    private String id;

    private String apiId;
    private Date createdAt;
    private Summary summary;

    @Builder.Default
    private List<Asset> assets = Collections.emptyList();

    public record Summary(Long errors, Long warnings, Long infos, Long hints) {}

    public record Asset(String pageId, String type, List<Diagnostic> diagnostics) {}

    public record Diagnostic(String severity, Range range, String rule, String message, String path) {}

    public record Range(Position start, Position end) {}

    public record Position(int line, int character) {}
}
