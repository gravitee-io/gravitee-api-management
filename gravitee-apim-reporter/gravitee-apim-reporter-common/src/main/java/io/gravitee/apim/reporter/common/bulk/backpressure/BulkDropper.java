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

import io.gravitee.apim.reporter.common.bulk.compressor.CompressedBulk;
import io.gravitee.apim.reporter.common.bulk.transformer.TransformedReport;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.NoArgsConstructor;

@CustomLog
@NoArgsConstructor
public class BulkDropper {

    private final Map<String, Integer> countPerType = new ConcurrentHashMap<>(8);

    public enum Reason {
        ERROR,
        OVERFLOW,
    }

    public void drop(CompressedBulk bulk, Reason reason) {
        count(bulk.countPerType());

        if (reason == Reason.OVERFLOW) {
            log.warn(
                "Overflow detected. Dropping bulk of reports to avoid too much memory pressure [{}]. Total: [{}].",
                bulk,
                countPerType
            );
        } else {
            log.warn("Bulk error detected. Dropping bulk of reports [{}]. Total: [{}].", bulk, countPerType);
        }
    }

    /**
     * Drops reports of a bulk that the target refused to write.
     *
     * @param reports the refused reports.
     * @param reasons why the target refused them, as given by the target.
     */
    public void drop(List<TransformedReport> reports, Collection<String> reasons) {
        final Map<String, Integer> rejected = reports
            .stream()
            .collect(Collectors.toMap(TransformedReport::type, report -> 1, Integer::sum));
        count(rejected);

        log.warn("Reports rejected by the target. Dropping reports [{}]. Reasons: {}. Total: [{}].", rejected, reasons, countPerType);
    }

    private void count(Map<String, Integer> dropped) {
        dropped.forEach((type, count) -> countPerType.merge(type, count, Integer::sum));
    }
}
