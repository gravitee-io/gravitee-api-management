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
package io.gravitee.rest.api.portal.rest.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.util.Arrays;
import org.junit.Test;

import io.gravitee.rest.api.model.analytics.Bucket;
import io.gravitee.rest.api.model.analytics.HistogramAnalytics;
import io.gravitee.rest.api.model.analytics.HitsAnalytics;
import io.gravitee.rest.api.model.analytics.Timestamp;
import io.gravitee.rest.api.model.analytics.TopHitsAnalytics;
import io.gravitee.rest.api.portal.rest.model.CountAnalytics;
import io.gravitee.rest.api.portal.rest.model.DateHistoAnalytics;
import io.gravitee.rest.api.portal.rest.model.GroupByAnalytics;
import io.gravitee.rest.api.portal.rest.model.Timerange;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AnalyticsMapperTest {

    private static final String ANALYTIC = "my-analytic";

    private AnalyticsMapper analyticsMapper = new AnalyticsMapper();
    
    @Test
    public void testConvertDateHisto() {
        HistogramAnalytics histogramAnalytics = new HistogramAnalytics();
        Timestamp timestamp = new Timestamp(1L, 2L, 3L);
        histogramAnalytics.setTimestamp(timestamp);
        
        Bucket b1 = new Bucket();
        final Number[] data1 = Arrays.array(1, 2, 3);
        b1.setData(data1);
        b1.setField("field1");
        b1.setName("name1");
        Map<String, Map<String, String>> metadata1 = new HashMap<>();
        Map<String, String> analyticMetadata1 = new HashMap<>();
        analyticMetadata1.put("key1", "value1");
        metadata1.put("bucket1", analyticMetadata1);
        b1.setMetadata(metadata1);
        
        Bucket b2 = new Bucket();
        final Number[] data2 = Arrays.array(4, 5, 6);
        b2.setData(data2);
        b2.setField("field2");
        b2.setName("name2");
        Map<String, Map<String, String>> metadata2 = new HashMap<>();
        Map<String, String> analyticMetadata2 = new HashMap<>();
        analyticMetadata2.put("key2", "value2");
        metadata2.put("bucket2", analyticMetadata2);
        b2.setMetadata(metadata2);
        b2.setBuckets(new ArrayList<Bucket>());
        
        Bucket b3 = new Bucket();
        final Number[] data3 = Arrays.array(7, 8, 9);
        b3.setData(data3);
        b3.setField("field3");
        b3.setName("name3");
        Map<String, Map<String, String>> metadata3 = new HashMap<>();
        Map<String, String> analyticMetadata3 = new HashMap<>();
        analyticMetadata3.put("key3", "value3");
        metadata3.put("bucket3", analyticMetadata3);
        b3.setMetadata(metadata3);
        List<Bucket> innerBucketList = new ArrayList<>();
        innerBucketList.add(b2);
        innerBucketList.add(b3);
        b1.setBuckets(innerBucketList);
        
        List<Bucket> bucketList = new ArrayList<>();
        bucketList.add(b1);
        histogramAnalytics.setValues(bucketList);
        
        //Test
        DateHistoAnalytics analytics = analyticsMapper.convert(histogramAnalytics);
        assertNotNull(analytics);
        final Timerange timeRange = analytics.getTimestamp();
        assertEquals(1, timeRange.getFrom().longValue());
        assertEquals(2, timeRange.getTo().longValue());
        assertEquals(3, timeRange.getInterval().longValue());
        
        final List<io.gravitee.rest.api.portal.rest.model.Bucket> values = analytics.getValues();
        assertNotNull(values);
        assertEquals(1, values.size());
        final io.gravitee.rest.api.portal.rest.model.Bucket bucket1 = values.get(0);
        assertEquals("field1", bucket1.getField());
        assertEquals(Arrays.asList(data1), bucket1.getData());
        assertEquals("name1", bucket1.getName());
        assertEquals(metadata1, bucket1.getMetadata());
        
        final List<io.gravitee.rest.api.portal.rest.model.Bucket> innerBuckets1 = bucket1.getBuckets();
        assertNotNull(innerBuckets1);
        assertEquals(2, innerBuckets1.size());
        final io.gravitee.rest.api.portal.rest.model.Bucket bucket2 = innerBuckets1.get(0);
        final io.gravitee.rest.api.portal.rest.model.Bucket bucket3 = innerBuckets1.get(1);

        assertEquals("field2", bucket2.getField());
        assertEquals(Arrays.asList(data2), bucket2.getData());
        assertEquals("name2", bucket2.getName());
        assertEquals(metadata2, bucket2.getMetadata());
        assertNotNull(bucket2.getBuckets());
        assertTrue(bucket2.getBuckets().isEmpty());
        
        assertEquals("field3", bucket3.getField());
        assertEquals(Arrays.asList(data3), bucket3.getData());
        assertEquals("name3", bucket3.getName());
        assertEquals(metadata3, bucket3.getMetadata());
        assertNotNull(bucket3.getBuckets());
        assertTrue(bucket3.getBuckets().isEmpty());

    }
    
    @Test
    public void testConvertCount() {
        HitsAnalytics hitsAnalytics = new HitsAnalytics();
        hitsAnalytics.setHits(1L);
        hitsAnalytics.setName("nameHits");
        
        //Test
        CountAnalytics analytics = analyticsMapper.convert(hitsAnalytics);
        assertNotNull(analytics);
        assertEquals(1, analytics.getHits().longValue());
        assertEquals("nameHits", analytics.getName());
    }
    
    @Test
    public void testConvertGroupBy() {
        TopHitsAnalytics topHitsAnalytics = new TopHitsAnalytics();
        
        Map<String, Map<String, String>> metadata = new HashMap<>();
        Map<String, String> analyticMetadata = new HashMap<>();
        analyticMetadata.put("key", "value");
        metadata.put("topHits", analyticMetadata);
        topHitsAnalytics.setMetadata(metadata);
        
        Map<String, Long> values = new HashMap<>();
        values.put("valuesKey", 1L);
        topHitsAnalytics.setValues(values);
        
        //Test
        GroupByAnalytics analytics = analyticsMapper.convert(topHitsAnalytics);
        assertNotNull(analytics);
        assertEquals(metadata, analytics.getMetadata());
        assertEquals(values, analytics.getValues());
    }
}
