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

    public static <T> Stream<T> toStream(ThrowingFunction<Pageable, Page<T>> pageSupplier) throws TechnicalException {
        var pageable = new PageableBuilder().pageSize(BATCH_PAGE_SIZE).pageNumber(0).build();
        var fistPage = pageSupplier.apply(pageable);

        if (fistPage == null || fistPage.getContent() == null) {
            return Stream.empty();
        }

        return Stream
            .iterate(
                fistPage,
                p -> !isEmpty(p),
                p -> {
                    try {
                        return hasNext(p) ? pageSupplier.apply(nextPageable(p, pageable)) : null;
                    } catch (TechnicalException e) {
                        throw new RuntimeException(e);
                    }
                }
            )
            .flatMap(p -> {
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
        return (page.getPageNumber() + 1) * page.getPageElements() < page.getTotalElements();
    }

    private static Pageable nextPageable(Page currentPage, Pageable pageable) {
        return new PageableBuilder().pageSize(pageable.pageSize()).pageNumber(currentPage.getPageNumber() + 1).build();
    }
}
