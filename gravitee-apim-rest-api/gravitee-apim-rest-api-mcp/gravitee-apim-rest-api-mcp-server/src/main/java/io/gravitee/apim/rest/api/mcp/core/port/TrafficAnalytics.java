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
package io.gravitee.apim.rest.api.mcp.core.port;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.rest.api.mcp.core.model.Dimension;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.apim.rest.api.mcp.core.model.TimelinePoint;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficSample;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficScope;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficStats;
import java.time.Duration;
import java.util.List;

/**
 * HTTP traffic of the caller's environment, restricted to the APIs the caller can see.
 */
public interface TrafficAnalytics {
    TrafficStats total(AuditInfo caller, TimeWindow window, TrafficScope scope);

    /** One sample per value of the dimension that received traffic in the window. */
    List<TrafficSample> by(AuditInfo caller, TimeWindow window, TrafficScope scope, Dimension dimension);

    /** One point per interval, oldest first. */
    List<TimelinePoint> timeline(AuditInfo caller, TimeWindow window, Duration interval, TrafficScope scope);
}
