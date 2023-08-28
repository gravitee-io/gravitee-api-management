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
package fixtures;

import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.logging.Logging;
import io.gravitee.definition.model.v4.analytics.sampling.Sampling;
import io.gravitee.definition.model.v4.analytics.sampling.SamplingType;

public class AnalyticsFixtures {

    private AnalyticsFixtures() {}

    public static Analytics anAnalytics() {
        final Analytics analytics = new Analytics();
        analytics.setEnabled(true);
        final Sampling sampling = new Sampling();
        sampling.setType(SamplingType.COUNT);
        sampling.setValue("10");
        analytics.setMessageSampling(sampling);
        analytics.setLogging(new Logging());
        return analytics;
    }

    public static io.gravitee.rest.api.management.v2.rest.model.Analytics aBasicAnalytics() {
        final io.gravitee.rest.api.management.v2.rest.model.Analytics analytics =
            new io.gravitee.rest.api.management.v2.rest.model.Analytics();
        analytics.setEnabled(true);
        final io.gravitee.rest.api.management.v2.rest.model.Sampling sampling =
            new io.gravitee.rest.api.management.v2.rest.model.Sampling();
        sampling.setType(io.gravitee.rest.api.management.v2.rest.model.Sampling.TypeEnum.COUNT);
        sampling.setValue("10");
        analytics.setSampling(sampling);
        analytics.setLogging(new io.gravitee.rest.api.management.v2.rest.model.LoggingV4());
        return analytics;
    }
}
