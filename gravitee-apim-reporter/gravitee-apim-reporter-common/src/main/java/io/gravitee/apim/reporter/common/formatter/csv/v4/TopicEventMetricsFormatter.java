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
package io.gravitee.apim.reporter.common.formatter.csv.v4;

import io.gravitee.reporter.api.v4.metric.event.TopicEventMetrics;
import io.vertx.core.buffer.Buffer;
import lombok.extern.slf4j.Slf4j;

/**
 * @author GraviteeSource Team
 */
@Slf4j
public class TopicEventMetricsFormatter extends BaseEventMetricsFormatter<TopicEventMetrics> {

    @Override
    protected Buffer format0(TopicEventMetrics data) {
        final Buffer buffer = super.format0(data);
        appendString(buffer, data.getTopic());

        //Append topic event metrics
        appendLong(buffer, data.getDownstreamPublishMessagesCountIncrement());
        appendLong(buffer, data.getDownstreamPublishMessageBytesIncrement());
        appendLong(buffer, data.getUpstreamPublishMessagesCountIncrement());
        appendLong(buffer, data.getUpstreamPublishMessageBytesIncrement());
        appendLong(buffer, data.getDownstreamSubscribeMessagesCountIncrement());
        appendLong(buffer, data.getDownstreamSubscribeMessageBytesIncrement());
        appendLong(buffer, data.getUpstreamSubscribeMessagesCountIncrement());
        appendLong(buffer, data.getUpstreamSubscribeMessageBytesIncrement());

        return buffer;
    }
}
