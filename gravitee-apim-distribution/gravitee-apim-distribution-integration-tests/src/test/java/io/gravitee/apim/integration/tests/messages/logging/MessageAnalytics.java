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
package io.gravitee.apim.integration.tests.messages.logging;

import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.logging.Logging;
import io.gravitee.definition.model.v4.analytics.logging.LoggingContent;
import io.gravitee.definition.model.v4.analytics.logging.LoggingMode;
import io.gravitee.definition.model.v4.analytics.logging.LoggingPhase;
import io.gravitee.definition.model.v4.analytics.sampling.Sampling;
import io.gravitee.definition.model.v4.analytics.sampling.SamplingType;

final class MessageAnalytics {

    private MessageAnalytics() {}

    /** Analytics on, connection and message logs on both legs, every message sampled. */
    static Analytics fullLogging() {
        var analytics = new Analytics();
        analytics.setEnabled(true);
        analytics.setLogging(
            Logging.builder()
                .mode(LoggingMode.builder().entrypoint(true).endpoint(true).build())
                .phase(LoggingPhase.builder().request(true).response(true).build())
                .content(LoggingContent.builder().messagePayload(true).build())
                .build()
        );
        var sampling = new Sampling();
        sampling.setType(SamplingType.COUNT);
        sampling.setValue("1");
        analytics.setMessageSampling(sampling);
        return analytics;
    }
}
