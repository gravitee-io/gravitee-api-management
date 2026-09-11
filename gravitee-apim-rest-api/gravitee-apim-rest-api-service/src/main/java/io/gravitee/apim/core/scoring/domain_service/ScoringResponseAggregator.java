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
package io.gravitee.apim.core.scoring.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.scoring.model.ScoringReport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@DomainService
public class ScoringResponseAggregator {

    private final Map<String, PendingScoringResponse> pendingResponses = new ConcurrentHashMap<>();

    public Optional<List<ScoringReport.Asset>> accumulate(String jobId, long expectedResponses, List<ScoringReport.Asset> assets) {
        if (expectedResponses <= 1) {
            return Optional.of(assets);
        }

        var pending = pendingResponses.computeIfAbsent(jobId, ignored ->
            new PendingScoringResponse((int) expectedResponses, new ArrayList<>())
        );

        synchronized (pending) {
            pending.assets().addAll(assets);
            pending.receivedResponses++;
            if (pending.receivedResponses < pending.expectedResponses) {
                return Optional.empty();
            }
            pendingResponses.remove(jobId);
            return Optional.of(mergeByPageId(pending.assets()));
        }
    }

    /**
     * Removes any pending response entry for the given job.
     * Should be called when a job is no longer in PENDING status (timed out, errored, or already completed)
     * to prevent memory leaks from accumulated partial responses.
     */
    public void cleanup(String jobId) {
        pendingResponses.remove(jobId);
    }

    private static List<ScoringReport.Asset> mergeByPageId(List<ScoringReport.Asset> assets) {
        Map<String, ScoringReport.Asset> merged = new LinkedHashMap<>();
        assets.forEach(asset -> merged.putIfAbsent(asset.pageId(), asset));
        return List.copyOf(merged.values());
    }

    private static final class PendingScoringResponse {

        private final int expectedResponses;
        private final List<ScoringReport.Asset> assets;
        private int receivedResponses;

        private PendingScoringResponse(int expectedResponses, List<ScoringReport.Asset> assets) {
            this.expectedResponses = expectedResponses;
            this.assets = assets;
        }

        private List<ScoringReport.Asset> assets() {
            return assets;
        }
    }
}
