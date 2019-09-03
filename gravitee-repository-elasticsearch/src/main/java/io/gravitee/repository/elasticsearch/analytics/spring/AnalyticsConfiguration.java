/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.elasticsearch.analytics.spring;

import io.gravitee.repository.analytics.api.AnalyticsRepository;
import io.gravitee.repository.analytics.query.stats.StatsQuery;
import io.gravitee.repository.elasticsearch.analytics.ElasticsearchAnalyticsRepository;
import io.gravitee.repository.elasticsearch.analytics.query.CountQueryCommand;
import io.gravitee.repository.elasticsearch.analytics.query.DateHistogramQueryCommand;
import io.gravitee.repository.elasticsearch.analytics.query.GroupByQueryCommand;
import io.gravitee.repository.elasticsearch.analytics.query.StatsQueryCommand;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class AnalyticsConfiguration {

    @Bean
    public AnalyticsRepository analyticsRepository() {
        return new ElasticsearchAnalyticsRepository();
    }

    @Bean
    public CountQueryCommand countQueryCommand() {
        return new CountQueryCommand();
    }

    @Bean
    public DateHistogramQueryCommand dateHistogramQueryCommand() {
        return new DateHistogramQueryCommand();
    }

    @Bean
    public GroupByQueryCommand groupByQueryCommand() {
        return new GroupByQueryCommand();
    }

    @Bean
    public StatsQueryCommand statsQueryCommand() {
        return new StatsQueryCommand();
    }
}
