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
package io.gravitee.repository.mongodb.management.internal.model;

import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author GraviteeSource Team
 */
@Document(collection = "#{@environment.getProperty('management.mongodb.prefix')}gamma_dashboards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class GammaDashboardMongo {

    @Id
    private String id;

    private String environmentId;
    private String title;
    private String description;
    private List<Filter> filters;
    private TimeRange timeRange;

    /** Raw, opaque widget JSON. Stored as a String so dotted map keys survive Mongo. */
    private String widgets;

    private String createdBy;
    private Date createdAt;
    private Date updatedAt;

    /** Deliberately not annotated {@code @Version}: this layer stores the counter, it does not manage it. */
    private Integer version;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Filter {

        private String field;
        private String label;
        private String operator;
        private List<String> value;
        private boolean editable;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeRange {

        private String type;
        private String period;
        private Long from;
        private Long to;
    }
}
