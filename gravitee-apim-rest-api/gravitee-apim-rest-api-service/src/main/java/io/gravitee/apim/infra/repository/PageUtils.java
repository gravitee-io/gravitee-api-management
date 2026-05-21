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
package io.gravitee.apim.infra.repository;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import java.util.stream.Stream;

public class PageUtils {

    public static final int BATCH_PAGE_SIZE = 400;

    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply(T t) throws TechnicalException;
    }

    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        void accept(T t) throws TechnicalException;
    }

    /**
     * Iterate every page produced by {@code pageSupplier} and feed it to {@code handler}, stopping when a page returns
     * fewer items than {@code pageSize}. Never reads {@link Page#getTotalElements()} — safe to use against repositories
     * that may return {@code -1} when their count operation timed out (APIM-14093).
     *
     * <p>Upgraders and other "must-complete" backend tasks should iterate pages through this helper rather than building
     * a {@code do { ... } while (handled < page.getTotalElements())} loop.
     */
    public static <T> void forEachPage(ThrowingFunction<Pageable, Page<T>> pageSupplier, int pageSize, ThrowingConsumer<Page<T>> handler)
        throws TechnicalException {
        int pageIdx = 0;
        boolean hasMore;
        do {
            Page<T> page = pageSupplier.apply(new PageableBuilder().pageSize(pageSize).pageNumber(pageIdx).build());
            if (page == null || page.getContent() == null || page.getContent().isEmpty()) {
                return;
            }
            handler.accept(page);
            hasMore = page.getContent().size() == pageSize;
            pageIdx++;
        } while (hasMore);
    }

    public static <T> Stream<T> toStream(ThrowingFunction<Pageable, Page<T>> pageSupplier) throws TechnicalException {
        var pageable = new PageableBuilder().pageSize(BATCH_PAGE_SIZE).pageNumber(0).build();
        var fistPage = pageSupplier.apply(pageable);

        if (fistPage == null || fistPage.getContent() == null) {
            return Stream.empty();
        }

        return Stream.iterate(
            fistPage,
            p -> !isEmpty(p),
            p -> {
                try {
                    return hasNext(p) ? pageSupplier.apply(nextPageable(p, pageable)) : null;
                } catch (TechnicalException e) {
                    throw new RuntimeException(e);
                }
            }
        ).flatMap(p -> {
            if (p != null && p.getContent() != null) {
                return p.getContent().stream();
            }
            return Stream.empty();
        });
    }

    private static boolean isEmpty(Page page) {
        return page == null || page.getContent() == null || page.getContent().isEmpty();
    }

    private static boolean hasNext(Page page) {
        // When the repository could not compute the total (count timed out), fall back to the page-fullness heuristic so we
        // don't silently truncate iteration. The page is treated as last when it returned fewer elements than the page size.
        if (page.getTotalElements() < 0) {
            return page.getContent() != null && page.getPageElements() > 0;
        }
        return (page.getPageNumber() + 1) * page.getPageElements() < page.getTotalElements();
    }

    private static Pageable nextPageable(Page currentPage, Pageable pageable) {
        return new PageableBuilder().pageSize(pageable.pageSize()).pageNumber(currentPage.getPageNumber() + 1).build();
    }
}
