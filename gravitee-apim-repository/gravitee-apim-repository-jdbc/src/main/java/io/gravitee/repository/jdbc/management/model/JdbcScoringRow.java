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
package io.gravitee.repository.jdbc.management.model;

import io.gravitee.repository.management.model.ScoringReport;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@Builder
public final class JdbcScoringRow {

    @EqualsAndHashCode.Include
    private String reportId;

    private String apiId;
    private String environmentId;
    private String pageId;
    private String type;
    private Date createdAt;
    private String severity;
    private Integer startLine;
    private Integer startCharacter;
    private Integer endLine;
    private Integer endCharacter;
    private String rule;
    private String message;
    private String path;

    private ScoringReport.Summary summary;

    public JdbcScoringRow(String reportId, String apiId, String environmentId, String pageId, String type, Date createdAt) {
        this.reportId = reportId;
        this.apiId = apiId;
        this.environmentId = environmentId;
        this.pageId = pageId;
        this.type = type;
        this.createdAt = createdAt;
    }
}
