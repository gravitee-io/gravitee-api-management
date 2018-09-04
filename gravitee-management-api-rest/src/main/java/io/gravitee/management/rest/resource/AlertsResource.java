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
package io.gravitee.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.alert.AlertMetric;
import io.gravitee.management.model.alert.AlertThreshold;
import io.gravitee.management.model.alert.MetricType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.List;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/alerts")
@Api(tags = {"Alerts"})
public class AlertsResource extends AbstractResource {

    @Path("metrics")
    @GET
    @ApiOperation(value = "List alert metrics")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AlertMetric> listMetrics() {
        return stream(MetricType.values()).map(metric -> {
            final AlertMetric alertMetric = new AlertMetric();
            alertMetric.setKey(metric.name().toLowerCase());
            alertMetric.setDescription(metric.description());
            alertMetric.setThresholds(metric.thresholds().stream().map(thresholdType -> {
                final AlertThreshold alertThreshold = new AlertThreshold();
                alertThreshold.setKey(thresholdType.name().toLowerCase());
                alertThreshold.setDescription(thresholdType.description());
                return alertThreshold;
            }).collect(toList()));
            return alertMetric;
        }).collect(toList());
    }
}
