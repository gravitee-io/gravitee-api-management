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
package io.gravitee.repository.elasticsearch.utils;

import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ClusterUtilsTest {
    @Mock
    RepositoryConfiguration configuration;

    /* No query */

    @Test
    public void shouldReturnEmpty_withNoQueryAndNoCluster() {
        when(configuration.hasCrossClusterMapping()).thenReturn(Boolean.FALSE);

        String[] cluster = ClusterUtils.extractClusterIndexPrefixes(configuration);
        assertNotNull(cluster);
        assertEquals(0, cluster.length);
    }

    @Test
    public void shouldReturnCluster_withNoQueryAndCluster() {
        when(configuration.hasCrossClusterMapping()).thenReturn(Boolean.TRUE);

        String[] cluster = ClusterUtils.extractClusterIndexPrefixes(configuration);
        assertNotNull(cluster);
        assertEquals(1, cluster.length);
        assertEquals("*", cluster[0]);
    }

    /* Analytics Query */

    @Test
    public void shouldReturnEmpty_withAnalyticsTenantAndNoCluster() {
        when(configuration.hasCrossClusterMapping()).thenReturn(Boolean.FALSE);
        io.gravitee.repository.analytics.query.AbstractQuery query = mock(io.gravitee.repository.analytics.query.AbstractQuery.class);

        String[] cluster = ClusterUtils.extractClusterIndexPrefixes(query, configuration);
        assertNotNull(cluster);
        assertEquals(0, cluster.length);
        verify(query, never()).query();
    }

    @Test
    public void shouldReturnCluster_withNoAnalyticsTenantAndCluster() {
        when(configuration.hasCrossClusterMapping()).thenReturn(Boolean.TRUE);
        io.gravitee.repository.analytics.query.AbstractQuery query = mock(io.gravitee.repository.analytics.query.AbstractQuery.class);
        io.gravitee.repository.analytics.query.QueryFilter qf = mock(io.gravitee.repository.analytics.query.QueryFilter.class);
        when(query.query()).thenReturn(qf);
        when(qf.filter()).thenReturn("");

        String[] cluster = ClusterUtils.extractClusterIndexPrefixes(query, configuration);
        assertNotNull(cluster);
        assertEquals(1, cluster.length);
        assertEquals("*", cluster[0]);
    }

    @Test
    public void shouldReturnCluster_withOneAnalyticsTenantAndCluster() {
        when(configuration.hasCrossClusterMapping()).thenReturn(Boolean.TRUE);
        io.gravitee.repository.analytics.query.AbstractQuery query = mock(io.gravitee.repository.analytics.query.AbstractQuery.class);
        io.gravitee.repository.analytics.query.QueryFilter qf = mock(io.gravitee.repository.analytics.query.QueryFilter.class);
        when(query.query()).thenReturn(qf);
        when(qf.filter()).thenReturn("(tenant:\\\"europe\\\")");
        when(configuration.getCrossClusterMapping()).thenReturn(Collections.singletonMap("europe", "europeCluster"));

        String[] cluster = ClusterUtils.extractClusterIndexPrefixes(query, configuration);
        assertNotNull(cluster);
        assertEquals(1, cluster.length);
        assertEquals("europeCluster", cluster[0]);
    }

    @Test
    public void shouldReturnCluster_withOneAnalyticsTenantAndClusterWithoutParenthesis() {
        when(configuration.hasCrossClusterMapping()).thenReturn(Boolean.TRUE);
        io.gravitee.repository.analytics.query.AbstractQuery query = mock(io.gravitee.repository.analytics.query.AbstractQuery.class);
        io.gravitee.repository.analytics.query.QueryFilter qf = mock(io.gravitee.repository.analytics.query.QueryFilter.class);
        when(query.query()).thenReturn(qf);
        when(qf.filter()).thenReturn("tenant:\\\"europe\\\"");
        when(configuration.getCrossClusterMapping()).thenReturn(Collections.singletonMap("europe", "europeCluster"));

        String[] cluster = ClusterUtils.extractClusterIndexPrefixes(query, configuration);
        assertNotNull(cluster);
        assertEquals(1, cluster.length);
        assertEquals("europeCluster", cluster[0]);
    }

    @Test
    public void shouldReturnCluster_withOneAnalyticsTenantAndClusterWithoutParenthesisAndPlan() {
        when(configuration.hasCrossClusterMapping()).thenReturn(Boolean.TRUE);
        io.gravitee.repository.analytics.query.AbstractQuery query = mock(io.gravitee.repository.analytics.query.AbstractQuery.class);
        io.gravitee.repository.analytics.query.QueryFilter qf = mock(io.gravitee.repository.analytics.query.QueryFilter.class);
        when(query.query()).thenReturn(qf);
        when(qf.filter()).thenReturn("tenant:\\\"europe\\\" AND plan:1234");
        when(configuration.getCrossClusterMapping()).thenReturn(Collections.singletonMap("europe", "europeCluster"));

        String[] cluster = ClusterUtils.extractClusterIndexPrefixes(query, configuration);
        assertNotNull(cluster);
        assertEquals(1, cluster.length);
        assertEquals("europeCluster", cluster[0]);
    }

    @Test
    public void shouldReturnCluster_withOneAnalyticsTenantAndClusterWithoutParenthesisAndPlanReverse() {
        when(configuration.hasCrossClusterMapping()).thenReturn(Boolean.TRUE);
        io.gravitee.repository.analytics.query.AbstractQuery query = mock(io.gravitee.repository.analytics.query.AbstractQuery.class);
        io.gravitee.repository.analytics.query.QueryFilter qf = mock(io.gravitee.repository.analytics.query.QueryFilter.class);
        when(query.query()).thenReturn(qf);
        when(qf.filter()).thenReturn("plan:1234 AND tenant:\\\"europe\\\"");
        when(configuration.getCrossClusterMapping()).thenReturn(Collections.singletonMap("europe", "europeCluster"));

        String[] cluster = ClusterUtils.extractClusterIndexPrefixes(query, configuration);
        assertNotNull(cluster);
        assertEquals(1, cluster.length);
        assertEquals("europeCluster", cluster[0]);
    }

    @Test
    public void shouldReturnCluster_withTwoAnalyticsTenantAndCluster() {
        when(configuration.hasCrossClusterMapping()).thenReturn(Boolean.TRUE);
        io.gravitee.repository.analytics.query.AbstractQuery query = mock(io.gravitee.repository.analytics.query.AbstractQuery.class);
        io.gravitee.repository.analytics.query.QueryFilter qf = mock(io.gravitee.repository.analytics.query.QueryFilter.class);
        when(query.query()).thenReturn(qf);
        when(qf.filter()).thenReturn("(tenant:\\\"europe\\\" OR \\\"india\\\")");
        Map<String, String> config = new HashMap<>();
        config.put("europe", "europeCluster");
        config.put("india", "indiaCluster");
        when(configuration.getCrossClusterMapping()).thenReturn(config);

        String[] cluster = ClusterUtils.extractClusterIndexPrefixes(query, configuration);
        assertNotNull(cluster);
        assertEquals(2, cluster.length);
        assertEquals("europeCluster", cluster[0]);
        assertEquals("indiaCluster", cluster[1]);
    }

    /* Healthcheck Query */

    @Test
    public void shouldReturnEmpty_withHealthCheckTenantAndNoCluster() {
        when(configuration.hasCrossClusterMapping()).thenReturn(Boolean.FALSE);
        io.gravitee.repository.healthcheck.query.AbstractQuery query = mock(io.gravitee.repository.healthcheck.query.AbstractQuery.class);

        String[] cluster = ClusterUtils.extractClusterIndexPrefixes(query, configuration);
        assertNotNull(cluster);
        assertEquals(0, cluster.length);
        verify(query, never()).query();
    }

    @Test
    public void shouldReturnCluster_withNoHealthCheckTenantAndCluster() {
        when(configuration.hasCrossClusterMapping()).thenReturn(Boolean.TRUE);
        io.gravitee.repository.healthcheck.query.AbstractQuery query = mock(io.gravitee.repository.healthcheck.query.AbstractQuery.class);
        io.gravitee.repository.healthcheck.query.QueryFilter qf = mock(io.gravitee.repository.healthcheck.query.QueryFilter.class);
        when(query.query()).thenReturn(qf);
        when(qf.filter()).thenReturn("");

        String[] cluster = ClusterUtils.extractClusterIndexPrefixes(query, configuration);
        assertNotNull(cluster);
        assertEquals(1, cluster.length);
        assertEquals("*", cluster[0]);
    }

    @Test
    public void shouldReturnCluster_withOneHealthCheckTenantAndCluster() {
        when(configuration.hasCrossClusterMapping()).thenReturn(Boolean.TRUE);
        io.gravitee.repository.analytics.query.AbstractQuery query = mock(io.gravitee.repository.analytics.query.AbstractQuery.class);
        io.gravitee.repository.analytics.query.QueryFilter qf = mock(io.gravitee.repository.analytics.query.QueryFilter.class);
        when(query.query()).thenReturn(qf);
        when(qf.filter()).thenReturn("tenant:(europe)");
        when(configuration.getCrossClusterMapping()).thenReturn(Collections.singletonMap("europe", "europeCluster"));

        String[] cluster = ClusterUtils.extractClusterIndexPrefixes(query, configuration);
        assertNotNull(cluster);
        assertEquals(1, cluster.length);
        assertEquals("europeCluster", cluster[0]);
    }

    @Test
    public void shouldReturnCluster_withTwoHealthCheckTenantAndCluster() {
        when(configuration.hasCrossClusterMapping()).thenReturn(Boolean.TRUE);
        io.gravitee.repository.analytics.query.AbstractQuery query = mock(io.gravitee.repository.analytics.query.AbstractQuery.class);
        io.gravitee.repository.analytics.query.QueryFilter qf = mock(io.gravitee.repository.analytics.query.QueryFilter.class);
        when(query.query()).thenReturn(qf);
        when(qf.filter()).thenReturn("tenant:(europe OR india)");
        Map<String, String> config = new HashMap<>();
        config.put("europe", "europeCluster");
        config.put("india", "indiaCluster");
        when(configuration.getCrossClusterMapping()).thenReturn(config);

        String[] cluster = ClusterUtils.extractClusterIndexPrefixes(query, configuration);
        assertNotNull(cluster);
        assertEquals(2, cluster.length);
        assertEquals("europeCluster", cluster[0]);
        assertEquals("indiaCluster", cluster[1]);
    }
}
