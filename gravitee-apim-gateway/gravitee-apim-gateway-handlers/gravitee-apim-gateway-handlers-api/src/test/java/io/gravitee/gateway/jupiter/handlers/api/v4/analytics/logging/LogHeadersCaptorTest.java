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
package io.gravitee.gateway.jupiter.handlers.api.v4.analytics.logging;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;

import io.gravitee.gateway.api.http.HttpHeaders;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LogHeadersCaptorTest {

    @Mock
    private HttpHeaders httpHeadersDelegate;

    private LogHeadersCaptor logHeadersCaptor;

    @BeforeEach
    public void beforeEach() {
        logHeadersCaptor = new LogHeadersCaptor(httpHeadersDelegate);
    }

    @Test
    void should_getDelegate() {
        // When
        HttpHeaders delegate = logHeadersCaptor.getDelegate();

        // Then
        assertThat(delegate).isSameAs(httpHeadersDelegate);
    }

    @Test
    void should_getCaptured() {
        // When
        HttpHeaders captured = logHeadersCaptor.getCaptured();

        // Then
        assertThat(captured).isNotSameAs(httpHeadersDelegate);
    }

    @Test
    void should_delegate_getFirst() {
        logHeadersCaptor.getFirst(null);

        // Then
        verify(httpHeadersDelegate).getFirst(null);
    }

    @Test
    void should_delegate_get() {
        logHeadersCaptor.get(null);

        // Then
        verify(httpHeadersDelegate).get(null);
    }

    @Test
    void should_delegate_getAll() {
        logHeadersCaptor.getAll(null);

        // Then
        verify(httpHeadersDelegate).getAll(null);
    }

    @Test
    void should_delegate_containsKey_charSequence() {
        logHeadersCaptor.containsKey((CharSequence) "name");

        // Then
        verify(httpHeadersDelegate).containsKey((CharSequence) "name");
    }

    @Test
    void should_delegate_containsKey() {
        logHeadersCaptor.containsKey("name");

        // Then
        verify(httpHeadersDelegate).containsKey("name");
    }

    @Test
    void should_delegate_contains() {
        logHeadersCaptor.contains("name");

        // Then
        verify(httpHeadersDelegate).contains("name");
    }

    @Test
    void should_delegate_contains_charSequence() {
        logHeadersCaptor.contains((CharSequence) "name");

        // Then
        verify(httpHeadersDelegate).contains((CharSequence) "name");
    }

    @Test
    void should_delegate_names() {
        logHeadersCaptor.names();

        // Then
        verify(httpHeadersDelegate).names();
    }

    @Test
    void should_delegate_add() {
        logHeadersCaptor.add("name", "value");

        // Then
        assertThat(logHeadersCaptor.getCaptured().contains("name")).isTrue();
        verify(httpHeadersDelegate).add("name", "value");
    }

    @Test
    void should_delegate_addList() {
        logHeadersCaptor.add("name", List.of("value"));

        // Then
        assertThat(logHeadersCaptor.getCaptured().contains("name")).isTrue();
        verify(httpHeadersDelegate).add("name", List.of("value"));
    }

    @Test
    void should_delegate_set() {
        logHeadersCaptor.set("name", "value");

        // Then
        assertThat(logHeadersCaptor.getCaptured().contains("name")).isTrue();
        verify(httpHeadersDelegate).set("name", "value");
    }

    @Test
    void should_delegate_setList() {
        logHeadersCaptor.set("name", List.of("value"));

        // Then
        assertThat(logHeadersCaptor.getCaptured().contains("name")).isTrue();
        verify(httpHeadersDelegate).set("name", List.of("value"));
    }

    @Test
    void should_delegate_remove() {
        logHeadersCaptor.set("name", "value");
        assertThat(logHeadersCaptor.getCaptured().contains("name")).isTrue();

        logHeadersCaptor.remove("name");

        // Then
        assertThat(logHeadersCaptor.getCaptured().contains("name")).isFalse();
        verify(httpHeadersDelegate).remove("name");
    }

    @Test
    void should_delegate_clear() {
        logHeadersCaptor.clear();

        // Then
        verify(httpHeadersDelegate).clear();
    }

    @Test
    void should_delegate_size() {
        logHeadersCaptor.size();

        // Then
        verify(httpHeadersDelegate).size();
    }

    @Test
    void should_delegate_isEmpty() {
        logHeadersCaptor.isEmpty();

        // Then
        verify(httpHeadersDelegate).isEmpty();
    }

    @Test
    void should_delegate_getOrDefault() {
        logHeadersCaptor.getOrDefault(null, null);

        // Then
        verify(httpHeadersDelegate).getOrDefault(null, null);
    }

    @Test
    void should_delegate_toSingleValueMap() {
        logHeadersCaptor.toSingleValueMap();

        // Then
        verify(httpHeadersDelegate).toSingleValueMap();
    }

    @Test
    void should_delegate_toListValuesMap() {
        logHeadersCaptor.toListValuesMap();

        // Then
        verify(httpHeadersDelegate).toListValuesMap();
    }

    @Test
    void should_delegate_containsAllKeys() {
        logHeadersCaptor.containsAllKeys(null);

        // Then
        verify(httpHeadersDelegate).containsAllKeys(null);
    }

    @Test
    void should_delegate_deeplyEquals() {
        logHeadersCaptor.deeplyEquals(null);

        // Then
        verify(httpHeadersDelegate).deeplyEquals(null);
    }

    @Test
    void should_delegate_iterator() {
        logHeadersCaptor.iterator();

        // Then
        verify(httpHeadersDelegate).iterator();
    }

    @Test
    void should_delegate_forEach() {
        logHeadersCaptor.forEach(null);

        // Then
        verify(httpHeadersDelegate).forEach(null);
    }

    @Test
    void should_delegate_spliterator() {
        logHeadersCaptor.spliterator();

        // Then
        verify(httpHeadersDelegate).spliterator();
    }
}
