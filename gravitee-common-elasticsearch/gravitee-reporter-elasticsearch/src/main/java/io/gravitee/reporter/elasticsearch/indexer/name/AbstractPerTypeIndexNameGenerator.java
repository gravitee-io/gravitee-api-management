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
package io.gravitee.reporter.elasticsearch.indexer.name;

import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.health.EndpointStatus;
import io.gravitee.reporter.api.http.Metrics;
import io.gravitee.reporter.api.log.Log;
import io.gravitee.reporter.api.monitor.Monitor;

import java.time.Instant;

/**
 * @author GraviteeSource Team
 */
public abstract class AbstractPerTypeIndexNameGenerator extends AbstractIndexNameGenerator {

    public abstract String generate(String type, Instant timestamp);

    @Override
    public String generate(Reportable reportable) {
        String type = null;
        if (reportable instanceof Metrics) {
            type = Type.REQUEST.getType();
        } else if (reportable instanceof Log) {
            type = Type.LOG.getType();
        } else if (reportable instanceof Monitor) {
            type = Type.MONITOR.getType();
        } else if (reportable instanceof EndpointStatus) {
            type = Type.HEALTH_CHECK.getType();
        }

        return generate(type, reportable.timestamp());
    }
}
