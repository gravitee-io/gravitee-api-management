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
package io.gravitee.rest.api.service.impl.configuration.dictionary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.diff.JsonDiff;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import org.mockito.ArgumentCaptor;

/**
 * The RFC-6902 patch for the given audit data, diffed the way {@code AuditServiceImpl} does it. Unlike the
 * production mapper, the plain mapper used here keeps null fields, so the patch may hold extra null-valued ops.
 */
final class DictionaryAuditPatch {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DictionaryAuditPatch() {}

    static AuditService.AuditLogData captured(AuditService auditService) {
        ArgumentCaptor<AuditService.AuditLogData> auditLogData = ArgumentCaptor.forClass(AuditService.AuditLogData.class);
        verify(auditService, atLeastOnce()).createAuditLog(any(ExecutionContext.class), auditLogData.capture());
        List<AuditService.AuditLogData> diffedEntries = auditLogData
            .getAllValues()
            .stream()
            .filter(DictionaryAuditPatch::isDiffed)
            .toList();
        assertThat(diffedEntries).hasSize(1);
        return diffedEntries.getFirst();
    }

    private static boolean isDiffed(AuditService.AuditLogData data) {
        return data.getPatch() == null;
    }

    static JsonNode capturedPatch(AuditService auditService) {
        return of(captured(auditService));
    }

    static JsonNode of(AuditService.AuditLogData data) {
        return JsonDiff.asJson(withoutTimestamps(data.getOldValue()), withoutTimestamps(data.getNewValue()));
    }

    static JsonNode json(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(json, e);
        }
    }

    private static ObjectNode withoutTimestamps(Object value) {
        if (value == null) {
            return MAPPER.createObjectNode();
        }
        ObjectNode node = MAPPER.valueToTree(value);
        node.remove(List.of("updatedAt", "createdAt"));
        return node;
    }
}
