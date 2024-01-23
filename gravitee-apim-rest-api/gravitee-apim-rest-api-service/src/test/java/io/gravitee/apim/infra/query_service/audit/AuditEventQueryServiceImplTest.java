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
package io.gravitee.apim.infra.query_service.audit;

import java.util.Comparator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class AuditEventQueryServiceImplTest {

    AuditEventQueryServiceImpl service = new AuditEventQueryServiceImpl();

    @Test
    void should_return_all_audit_events_names_sorted() {
        var result = service.listAllApiAuditEvents();

        Assertions.assertThat(result).hasSize(48).isSortedAccordingTo(Comparator.naturalOrder());
    }

    @Test
    void should_cache_result() {
        var result = service.listAllApiAuditEvents();
        var result2 = service.listAllApiAuditEvents();

        Assertions.assertThat(result2).isSameAs(result);
    }
}
