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
package io.gravitee.apim.reporter.common.bulk.backpressure;

/**
 * Told about every report a {@link BulkDropper} drops, so that a reporter can expose the losses as a metric.
 */
@FunctionalInterface
public interface DroppedReportsListener {
    DroppedReportsListener NONE = (reason, type, count) -> {};

    /**
     * @param type the type of the dropped reports, as {@link io.gravitee.apim.reporter.common.MetricsType#getType()} names it
     */
    void dropped(BulkDropper.Reason reason, String type, int count);
}
