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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.gravitee.rest.api.model.analytics.HistogramAnalytics;
import io.gravitee.rest.api.model.analytics.HitsAnalytics;
import io.gravitee.rest.api.model.analytics.TopHitsAnalytics;
import io.gravitee.rest.api.portal.rest.model.Bucket;
import io.gravitee.rest.api.portal.rest.model.CountAnalytics;
import io.gravitee.rest.api.portal.rest.model.DateHistoAnalytics;
import io.gravitee.rest.api.portal.rest.model.GroupByAnalytics;
import io.gravitee.rest.api.portal.rest.model.Timerange;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */

@Component
public class AnalyticsMapper {
    public DateHistoAnalytics convert(HistogramAnalytics analytics) {
       DateHistoAnalytics analyticsItem = new DateHistoAnalytics();
       analyticsItem.setTimestamp(new Timerange()
               .from(analytics.getTimestamp().getFrom())
               .to(analytics.getTimestamp().getTo())
               .interval(analytics.getTimestamp().getInterval())
               );
       List<Bucket> buckets = convertBucketList(analytics.getValues());
       analyticsItem.setValues(buckets);
       
       return analyticsItem;
    }

    private List<Bucket> convertBucketList(List<io.gravitee.rest.api.model.analytics.Bucket> buckets) {
        if(buckets != null && !buckets.isEmpty()) {
            return buckets.stream()
                    .map(b-> new Bucket()
                            .data(Arrays.asList(b.getData()))
                            .field(b.getField())
                            .metadata(b.getMetadata())
                            .name(b.getName())
                            .buckets(this.convertBucketList(b.getBuckets()))
                        )
                    .collect(Collectors.toList())
                    ;
        }
        return Collections.emptyList();
    }

    public CountAnalytics convert(HitsAnalytics analytics) {
        CountAnalytics analyticsItem = new CountAnalytics();
        analyticsItem.setHits(analytics.getHits());
        return analyticsItem;
    }

    public GroupByAnalytics convert(TopHitsAnalytics analytics) {
        GroupByAnalytics analyticsItem = new GroupByAnalytics();
        analyticsItem.setMetadata(analytics.getMetadata());
        analyticsItem.setValues(analytics.getValues());
        return analyticsItem;
    }
}
