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
package io.gravitee.rest.api.service.impl.search;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.search.IndexationObserver;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.impl.search.lucene.DocumentTransformer;
import io.gravitee.rest.api.service.impl.search.lucene.SearchEngineIndexer;
import java.util.List;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Locks the IndexationObserver dispatcher invariants on SearchEngineServiceImpl:
 *   - observer fires BEFORE the Lucene write, on every index/delete call
 *   - observer STILL fires when the Lucene write subsequently throws (path-index integrity > search index)
 *   - observer STILL fires when no transformer matches the Indexable (other observer types may still care)
 *   - delete(ctx, source, locally=false) deletes locally on the originating node THEN broadcasts
 *
 * The path index is the collision-detection source of truth; it must never be left stale because Lucene degraded.
 */
class SearchEngineServiceImplObserverTest {

    private final SearchEngineIndexer luceneIndexer = mock(SearchEngineIndexer.class);
    private final IndexationObserver observer = mock(IndexationObserver.class);
    private final DocumentTransformer<Indexable> transformer = mock(DocumentTransformer.class);
    private SearchEngineServiceImpl service;

    @BeforeEach
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void setUp() {
        service = new SearchEngineServiceImpl();
        ReflectionTestUtils.setField(service, "indexer", luceneIndexer);
        ReflectionTestUtils.setField(service, "transformers", (List) List.of(transformer));
        ReflectionTestUtils.setField(service, "indexationObservers", List.of(observer));
        when(transformer.handle(any())).thenReturn(true);
        when(transformer.transform(any())).thenReturn(new Document());
    }

    @Test
    void index_fires_observer_on_every_call() throws TechnicalException {
        var source = mock(Indexable.class);

        service.index(new ExecutionContext("org", "env"), source, true);

        verify(luceneIndexer).index(any(), eq(true));
        verify(observer).onIndex(source);
    }

    @Test
    void index_still_fires_observer_when_lucene_write_throws() throws TechnicalException {
        var source = mock(Indexable.class);
        doThrow(new TechnicalException("simulated lucene failure")).when(luceneIndexer).index(any(), anyBoolean());

        service.index(new ExecutionContext("org", "env"), source, true);

        verify(observer, times(1)).onIndex(source);
    }

    @Test
    void index_still_fires_observer_when_no_transformer_matches() {
        when(transformer.handle(any())).thenReturn(false);
        var source = mock(Indexable.class);

        service.index(new ExecutionContext("org", "env"), source, true);

        verify(observer, times(1)).onIndex(source);
    }

    @Test
    void observer_failure_does_not_block_lucene_write() throws TechnicalException {
        var source = mock(Indexable.class);
        doThrow(new IllegalStateException("observer boom")).when(observer).onIndex(source);

        service.index(new ExecutionContext("org", "env"), source, true);

        verify(observer, times(1)).onIndex(source);
        verify(luceneIndexer, times(1)).index(any(), eq(true));
    }

    @Test
    void delete_locally_false_fires_observer_immediately_skips_local_lucene_and_broadcasts() throws TechnicalException {
        // Lucene-side: preserves Antoine Cordier's 2021 'avoid duplicated deletion' optimisation (commit 6ce444f).
        // The originating node skips its local Lucene remove because its own cron poller will pick up the broadcast
        // and run deleteLocally once.
        // Observer-side: fires immediately on the originator anyway so caches like ApiPathIndex don't lag behind
        // Mongo by a cron interval — clients deleting then recreating within ms would otherwise hit a stale "Path
        // already exists" error. The cron-driven deleteLocally later invokes onDelete a second time; observer
        // effects must be idempotent.
        var commandService = mock(io.gravitee.rest.api.service.CommandService.class);
        ReflectionTestUtils.setField(service, "commandService", commandService);
        var source = mock(Indexable.class);

        service.delete(new ExecutionContext("org", "env"), source, false);

        verify(luceneIndexer, org.mockito.Mockito.never()).remove(any());
        verify(observer, times(1)).onDelete(source);
        verify(commandService, times(1)).send(any(), any());
    }

    @Test
    void delete_locally_true_runs_locally_without_broadcast() throws TechnicalException {
        var commandService = mock(io.gravitee.rest.api.service.CommandService.class);
        ReflectionTestUtils.setField(service, "commandService", commandService);
        var source = mock(Indexable.class);

        service.delete(new ExecutionContext("org", "env"), source, true);

        verify(luceneIndexer, times(1)).remove(any());
        verify(observer, times(1)).onDelete(source);
        verify(commandService, org.mockito.Mockito.never()).send(any(), any());
    }

    @Test
    void one_throwing_observer_does_not_skip_subsequent_observers() throws TechnicalException {
        // Observers are independent: a failure in one must not block the next from receiving the event. Otherwise
        // registration order silently determines which downstream caches stay in sync, which is a sharp edge.
        var failing = mock(IndexationObserver.class);
        var stillCalled = mock(IndexationObserver.class);
        doThrow(new IllegalStateException("first observer boom")).when(failing).onIndex(any());
        ReflectionTestUtils.setField(service, "indexationObservers", List.of(failing, stillCalled));
        var source = mock(Indexable.class);

        service.index(new ExecutionContext("org", "env"), source, true);

        verify(failing, times(1)).onIndex(source);
        verify(stillCalled, times(1)).onIndex(source);
        verify(luceneIndexer, times(1)).index(any(), eq(true));
    }

    @Test
    void one_throwing_observer_does_not_skip_subsequent_observers_on_delete() throws TechnicalException {
        var failing = mock(IndexationObserver.class);
        var stillCalled = mock(IndexationObserver.class);
        doThrow(new IllegalStateException("first observer boom")).when(failing).onDelete(any());
        ReflectionTestUtils.setField(service, "indexationObservers", List.of(failing, stillCalled));
        var source = mock(Indexable.class);

        service.delete(new ExecutionContext("org", "env"), source, true);

        verify(failing, times(1)).onDelete(source);
        verify(stillCalled, times(1)).onDelete(source);
        verify(luceneIndexer, times(1)).remove(any());
    }
}
