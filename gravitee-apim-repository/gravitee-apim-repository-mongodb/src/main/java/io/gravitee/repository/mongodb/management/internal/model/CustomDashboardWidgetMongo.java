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

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomDashboardWidgetMongo {

    private String id;
    private String title;
    private String type;
    private Layout layout;
    private Request request;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Layout {

        private int cols;
        private int rows;
        private int x;
        private int y;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeRange {

        private String from;
        private String to;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricRequest {

        private String name;
        private List<String> measures;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {

        private String type;
        private TimeRange timeRange;
        private List<MetricRequest> metrics;
        private Long interval;
        private List<String> by;
        private Integer limit;
    }
}
