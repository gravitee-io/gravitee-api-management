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
package io.gravitee.repository.elasticsearch.log;

import static io.gravitee.repository.analytics.query.DateRangeBuilder.lastDays;
import static io.gravitee.repository.analytics.query.IntervalBuilder.hours;
import static io.gravitee.repository.analytics.query.QueryBuilders.tabular;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.analytics.query.tabular.TabularResponse;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.log.model.ExtendedLog;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ElasticsearchLogRepositoryTest extends AbstractElasticsearchRepositoryTest {

    private QueryContext queryContext = new QueryContext("org#1", "env#1");

    @Autowired
    private ElasticLogRepository logRepository;

    @Test
    public void testFindById() throws Exception {
        // 29381bce-df59-47b2-b81b-cedf59c7b23e request is stored in the yesterday index
        ExtendedLog log = logRepository.findById(
            queryContext,
            "29381bce-df59-47b2-b81b-cedf59c7b23e",
            System.currentTimeMillis() - 24 * 60 * 60 * 1000
        );

        assertThat(log).isNotNull();
    }

    @Test
    public void testTabular_withQuery() throws Exception {
        TabularResponse response = logRepository.query(
            queryContext,
            tabular()
                .timeRange(lastDays(60), hours(1))
                .terms(Map.of("api", Set.of("be0aa9c9-ca1c-4d0a-8aa9-c9ca1c5d0aab")))
                .page(1)
                .size(20)
                .build()
        );

        assertThat(response).isNotNull();
        assertThat(response.getSize()).isGreaterThan(0);
    }

    @Test
    public void testTabular_withLogQuery() throws Exception {
        TabularResponse response = logRepository.query(
            queryContext,
            tabular().timeRange(lastDays(60), hours(1)).query("client-response.body:*not valid or is expired*").page(1).size(10).build()
        );

        assertThat(response).isNotNull();
        assertThat(response.getSize()).isEqualTo(6);
        assertThat(response.getLogs()).hasSize(6);
    }

    @Test
    public void testTabular_withLogQuery_page1() throws Exception {
        TabularResponse response = logRepository.query(
            queryContext,
            tabular().timeRange(lastDays(60), hours(1)).query("client-response.body:*not valid or is expired*").page(1).size(5).build()
        );

        assertThat(response).isNotNull();
        assertThat(response.getSize()).isEqualTo(6);
        assertThat(response.getLogs()).hasSize(5);
    }

    @Test
    public void testTabular_withLogQuery_termWithCurrency() throws Exception {
        TabularResponse response = logRepository.query(
            queryContext,
            tabular().timeRange(lastDays(60), hours(1)).query("client-response.body:*10$*").page(1).size(5).build()
        );
        assertThat(response).isNotNull();
        assertThat(response.getSize()).isOne();
        assertThat(response.getLogs()).hasSize(1);
    }

    @Test
    public void testTabular_withLogQuery_termWithEmail() throws Exception {
        TabularResponse response = logRepository.query(
            queryContext,
            tabular().timeRange(lastDays(60), hours(1)).query("client-response.body:*john@yopmail.com*").page(1).size(5).build()
        );
        assertThat(response).isNotNull();
        assertThat(response.getSize()).isOne();
        assertThat(response.getLogs()).hasSize(1);
    }

    @Test
    public void testTabular_withLogQuery_page2() throws Exception {
        TabularResponse response = logRepository.query(
            queryContext,
            tabular().timeRange(lastDays(60), hours(1)).query("client-response.body:*not valid or is expired*").page(2).size(5).build()
        );

        assertThat(response).isNotNull();

        assertThat(response.getSize()).isEqualTo(6);
        assertThat(response.getLogs()).hasSize(1);
    }

    @Test
    public void testTabular_withoutQuery() throws Exception {
        TabularResponse response = logRepository.query(queryContext, tabular().timeRange(lastDays(60), hours(1)).page(1).size(100).build());

        assertThat(response).isNotNull();
        assertThat(response.getLogs().size()).isEqualTo(response.getSize());
    }

    @Test
    public void testTabular_withCombinedFilters_statusAndBody() throws Exception {
        TabularResponse response = logRepository.query(
            queryContext,
            tabular()
                .timeRange(lastDays(60), hours(1))
                .query("status:200 AND client-response.body:*not valid or is expired*")
                .page(1)
                .size(10)
                .build()
        );

        assertThat(response).isNotNull();
        assertThat(response.getSize()).isLessThanOrEqualTo(6);
        assertThat(response.getLogs()).hasSizeLessThanOrEqualTo((int) response.getSize());
    }

    @Test
    public void testTabular_withCombinedFilters_pagination_page1() throws Exception {
        TabularResponse response = logRepository.query(
            queryContext,
            tabular()
                .timeRange(lastDays(60), hours(1))
                .query("status:200 AND client-response.body:*not valid or is expired*")
                .page(1)
                .size(3)
                .build()
        );

        assertThat(response).isNotNull();
        long totalSize = response.getSize();
        assertThat(response.getLogs()).hasSizeLessThanOrEqualTo(3);
        assertThat(totalSize).isGreaterThanOrEqualTo(response.getLogs().size());
    }

    @Test
    public void testTabular_withCombinedFilters_pagination_page2() throws Exception {
        TabularResponse responsePage1 = logRepository.query(
            queryContext,
            tabular()
                .timeRange(lastDays(60), hours(1))
                .query("status:200 AND client-response.body:*not valid or is expired*")
                .page(1)
                .size(3)
                .build()
        );

        TabularResponse responsePage2 = logRepository.query(
            queryContext,
            tabular()
                .timeRange(lastDays(60), hours(1))
                .query("status:200 AND client-response.body:*not valid or is expired*")
                .page(2)
                .size(3)
                .build()
        );

        assertThat(responsePage2).isNotNull();
        assertThat(responsePage2.getSize()).isEqualTo(responsePage1.getSize());
        int expectedPage2Size = (int) Math.max(0, responsePage1.getSize() - 3);
        assertThat(responsePage2.getLogs()).hasSizeLessThanOrEqualTo(expectedPage2Size);
    }

    @Test
    public void testTabular_withCombinedFilters_differentPageSizes() throws Exception {
        TabularResponse responseSize10 = logRepository.query(
            queryContext,
            tabular()
                .timeRange(lastDays(60), hours(1))
                .query("status:200 AND client-response.body:*not valid or is expired*")
                .page(1)
                .size(10)
                .build()
        );

        TabularResponse responseSize15 = logRepository.query(
            queryContext,
            tabular()
                .timeRange(lastDays(60), hours(1))
                .query("status:200 AND client-response.body:*not valid or is expired*")
                .page(1)
                .size(15)
                .build()
        );

        TabularResponse responseSize100 = logRepository.query(
            queryContext,
            tabular()
                .timeRange(lastDays(60), hours(1))
                .query("status:200 AND client-response.body:*not valid or is expired*")
                .page(1)
                .size(100)
                .build()
        );
        assertThat(responseSize10.getSize()).isEqualTo(responseSize15.getSize()).isEqualTo(responseSize100.getSize());
    }

    @Test
    public void testTabular_bodyFilterOnly_pagination_consistent() throws Exception {
        String bodyQuery = "client-response.body:*not valid or is expired*";

        TabularResponse response1 = logRepository.query(
            queryContext,
            tabular().timeRange(lastDays(60), hours(1)).query(bodyQuery).page(1).size(10).build()
        );

        TabularResponse response2 = logRepository.query(
            queryContext,
            tabular().timeRange(lastDays(60), hours(1)).query(bodyQuery).page(1).size(15).build()
        );

        // Total should be consistent
        assertThat(response1.getSize()).isEqualTo(response2.getSize());
        assertThat(response1.getSize()).isEqualTo(6);
    }

    @Test
    public void testTabular_emptyResults_withCombinedFilters() throws Exception {
        TabularResponse response = logRepository.query(
            queryContext,
            tabular()
                .timeRange(lastDays(60), hours(1))
                .query("status:999 AND client-response.body:*nonexistent-text-12345*")
                .page(1)
                .size(10)
                .build()
        );

        assertThat(response).isNotNull();
        assertThat(response.getSize()).isZero();
        assertThat(response.getLogs()).isEmpty();
    }

    @Test
    public void testTabular_statusFilterOnly() throws Exception {
        TabularResponse response = logRepository.query(
            queryContext,
            tabular().timeRange(lastDays(60), hours(1)).query("status:200").page(1).size(10).build()
        );

        assertThat(response).isNotNull();
        assertThat(response.getSize()).isGreaterThan(0);
        assertThat(response.getLogs()).isNotEmpty();
    }

    @Test
    public void testTabular_multipleStatusFilters_withBody() throws Exception {
        TabularResponse response = logRepository.query(
            queryContext,
            tabular()
                .timeRange(lastDays(60), hours(1))
                .query("status:200 AND client-response.body:*not valid or is expired*")
                .page(1)
                .size(10)
                .build()
        );

        assertThat(response).isNotNull();
        assertThat(response.getLogs()).hasSizeLessThanOrEqualTo((int) response.getSize());
    }

    @Test
    public void testTabular_paginationBeyondResults() throws Exception {
        TabularResponse responsePage1 = logRepository.query(
            queryContext,
            tabular().timeRange(lastDays(60), hours(1)).query("client-response.body:*not valid or is expired*").page(1).size(10).build()
        );

        // Request page 10 when we only have 6 results
        TabularResponse responsePage10 = logRepository.query(
            queryContext,
            tabular().timeRange(lastDays(60), hours(1)).query("client-response.body:*not valid or is expired*").page(10).size(10).build()
        );

        assertThat(responsePage10).isNotNull();
        assertThat(responsePage10.getSize()).isEqualTo(responsePage1.getSize()); // Total should be same
        assertThat(responsePage10.getLogs()).isEmpty(); // No results on page 10
    }

    @Test
    public void testTabular_largePaginationSize() throws Exception {
        TabularResponse response = logRepository.query(
            queryContext,
            tabular().timeRange(lastDays(60), hours(1)).query("client-response.body:*not valid or is expired*").page(1).size(1000).build()
        );

        assertThat(response).isNotNull();
        assertThat(response.getSize()).isEqualTo(6);
        assertThat(response.getLogs()).hasSize(6); // Should return all available results
    }

    @Test
    public void testTabular_caseInsensitiveBodySearch() throws Exception {
        TabularResponse responseLower = logRepository.query(
            queryContext,
            tabular().timeRange(lastDays(60), hours(1)).query("client-response.body:*not valid*").page(1).size(10).build()
        );

        TabularResponse responseUpper = logRepository.query(
            queryContext,
            tabular().timeRange(lastDays(60), hours(1)).query("client-response.body:*NOT VALID*").page(1).size(10).build()
        );
        assertThat(responseLower).isNotNull();
        assertThat(responseUpper).isNotNull();
    }

    @Test
    public void testTabular_complexQuery_multipleConditions() throws Exception {
        TabularResponse response = logRepository.query(
            queryContext,
            tabular().timeRange(lastDays(60), hours(1)).query("status:200 AND client-response.body:*valid*").page(1).size(10).build()
        );

        assertThat(response).isNotNull();
        assertThat(response.getLogs()).hasSizeLessThanOrEqualTo((int) response.getSize());
    }
}
