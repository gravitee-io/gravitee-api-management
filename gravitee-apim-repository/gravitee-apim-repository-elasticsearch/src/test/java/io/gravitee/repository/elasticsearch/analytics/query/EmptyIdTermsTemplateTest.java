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
package io.gravitee.repository.elasticsearch.analytics.query;

import static io.gravitee.repository.analytics.query.DateRangeBuilder.lastDay;
import static io.gravitee.repository.analytics.query.IntervalBuilder.hours;
import static org.assertj.core.api.Assertions.assertThat;

import freemarker.template.Configuration;
import freemarker.template.Template;
import io.gravitee.repository.analytics.query.QueryBuilders;
import io.gravitee.repository.analytics.query.count.CountQueryBuilder;
import io.gravitee.repository.analytics.query.tabular.TabularQueryBuilder;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EmptyIdTermsTemplateTest {

    private static Configuration configuration;

    @BeforeAll
    static void loadTemplates() {
        configuration = new Configuration(Configuration.VERSION_2_3_32);
        configuration.setClassLoaderForTemplateLoading(EmptyIdTermsTemplateTest.class.getClassLoader(), "/freemarker");
        configuration.setDefaultEncoding("UTF-8");
    }

    @Test
    void should_match_nothing_when_a_count_term_has_no_values() throws Exception {
        var query = CountQueryBuilder.query().terms(Map.of("api", Set.of())).build();

        assertThat(render("analytics/count.ftl", query)).contains("match_none");
    }

    @Test
    void should_keep_a_count_term_that_has_values() throws Exception {
        var query = CountQueryBuilder.query().terms(Map.of("api", Set.of("api-1"))).build();

        String rendered = render("analytics/count.ftl", query);
        assertThat(rendered).contains("api-1");
        assertThat(rendered).doesNotContain("match_none");
    }

    @Test
    void should_match_nothing_when_a_log_term_has_no_values() throws Exception {
        var query = TabularQueryBuilder.query().page(1).size(10).timeRange(lastDay(), hours(1)).terms(Map.of("api", Set.of())).build();

        assertThat(render("log/log.ftl", query)).contains("match_none");
    }

    @Test
    void should_match_nothing_when_a_stats_term_has_no_values() throws Exception {
        var query = QueryBuilders.stats().field("response-time").timeRange(lastDay(), hours(1)).terms(Map.of("api", Set.of())).build();

        assertThat(render("analytics/stats.ftl", query)).contains("match_none");
    }

    @Test
    void should_keep_a_stats_term_that_has_values() throws Exception {
        var query = QueryBuilders.stats()
            .field("response-time")
            .timeRange(lastDay(), hours(1))
            .terms(Map.of("api", Set.of("api-1")))
            .build();

        String rendered = render("analytics/stats.ftl", query);
        assertThat(rendered).contains("api-1");
        assertThat(rendered).doesNotContain("match_none");
    }

    @Test
    void should_match_nothing_when_a_group_by_term_has_no_values() throws Exception {
        var query = QueryBuilders.groupBy().field("api").timeRange(lastDay(), hours(1)).terms(Map.of("api", Set.of())).build();

        assertThat(render("analytics/groupBy.ftl", query)).contains("match_none");
    }

    @Test
    void should_match_nothing_when_a_date_histogram_term_has_no_values() throws Exception {
        var query = QueryBuilders.dateHistogram().timeRange(lastDay(), hours(1)).terms(Map.of("api", Set.of())).build();

        assertThat(render("analytics/dateHistogram.ftl", query, Map.of("roundedFrom", 0L, "roundedTo", 1L))).contains("match_none");
    }

    private static String render(String templateName, Object query) throws Exception {
        return render(templateName, query, Map.of());
    }

    private static String render(String templateName, Object query, Map<String, Object> extraData) throws Exception {
        Template template = configuration.getTemplate(templateName);
        Map<String, Object> data = new HashMap<>(extraData);
        data.put("query", query);
        StringWriter writer = new StringWriter();
        template.process(data, writer);
        return writer.toString();
    }
}
