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
package io.gravitee.apim.reporter.common.bulk.compressor;

import io.gravitee.apim.reporter.common.bulk.transformer.TransformedReport;
import io.reactivex.rxjava3.annotations.NonNull;
import io.vertx.rxjava3.core.buffer.Buffer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NoArgsConstructor;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
public class NoneBulkCompressor implements BulkCompressor {

    @Override
    public CompressedBulk compress(List<@NonNull TransformedReport> reports) throws IOException {
        final Map<String, Integer> countPerType = new HashMap<>(8);
        final Buffer compressed = Buffer.buffer(
            reports
                .stream()
                .mapToInt(report -> report.transformed().length())
                .sum()
        );
        for (TransformedReport report : reports) {
            countPerType.compute(report.type(), (clazz, integer) -> integer != null ? ++integer : 1);
            compressed.appendBuffer(report.transformed());
        }
        return new CompressedBulk(compressed, countPerType, sliceReports(compressed, reports));
    }

    // Slices share the bytes of the bulk, so keeping the reports does not hold a second copy of them in memory.
    private static List<TransformedReport> sliceReports(Buffer compressed, List<TransformedReport> reports) {
        final List<TransformedReport> slices = new ArrayList<>(reports.size());
        int start = 0;
        for (TransformedReport report : reports) {
            final int end = start + report.transformed().length();
            slices.add(new TransformedReport(compressed.slice(start, end), report.clazz()));
            start = end;
        }
        return slices;
    }
}
