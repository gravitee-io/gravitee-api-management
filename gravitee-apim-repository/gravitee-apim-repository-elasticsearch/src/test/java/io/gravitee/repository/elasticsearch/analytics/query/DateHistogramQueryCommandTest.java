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
package io.gravitee.repository.elasticsearch.analytics.query;

import static io.reactivex.Single.just;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.index.IndexNameGenerator;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.templating.freemarker.FreeMarkerComponent;
import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.gravitee.elasticsearch.version.Version;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.analytics.query.DateHistogramQuery;
import io.gravitee.repository.analytics.query.DateRange;
import io.gravitee.repository.analytics.query.TimeRangeFilter;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class DateHistogramQueryCommandTest {

    @InjectMocks
    private DateHistogramQueryCommand dateHistogramQueryCommand = new DateHistogramQueryCommand();

    @Mock
    private FreeMarkerComponent freeMarkerComponent;

    @Mock
    private RepositoryConfiguration repositoryConfiguration;

    @Mock
    private IndexNameGenerator indexNameGenerator;

    @Mock
    private Client client;

    @Mock
    private DateHistogramQuery dateHistogramQuery;

    @Mock
    private TimeRangeFilter timeRangeFilter;

    @Mock
    private DateRange dateRange;

    @Mock
    protected ElasticsearchInfo info;

    @Mock
    protected Version version;

    @Test
    public void shouldExecuteQuery() throws AnalyticsException {
        when(freeMarkerComponent.generateFromTemplate(anyString(), anyMap())).thenReturn("");
        when(client.search(isNull(), anyString(), anyString())).thenReturn(just(new SearchResponse()));
        when(dateHistogramQuery.timeRange()).thenReturn(timeRangeFilter);
        when(timeRangeFilter.range()).thenReturn(dateRange);
        when(info.getVersion()).thenReturn(version);
        when(version.canUseTypeRequests()).thenReturn(true);

        checkDateRange(1561477219132L, 1564069219132L, 600000);
        checkDateRange(1561477219132L, 1564069219132L, 43200000);
    }

    private void checkDateRange(final long from, final long to, final int interval) throws AnalyticsException {
        when(timeRangeFilter.interval()).thenReturn(() -> interval);
        when(dateRange.from()).thenReturn(from);
        when(dateRange.to()).thenReturn(to);
        dateHistogramQueryCommand.executeQuery(dateHistogramQuery);

        verify(freeMarkerComponent)
            .generateFromTemplate(
                anyString(),
                argThat(
                    argument -> {
                        final Long roundedFrom = (Long) argument.get("roundedFrom");
                        final Long roundedTo = (Long) argument.get("roundedTo");
                        return roundedFrom.compareTo(from) <= 0 && roundedTo.compareTo(to) >= 0;
                    }
                )
            );
        clearInvocations(freeMarkerComponent);
    }
}
