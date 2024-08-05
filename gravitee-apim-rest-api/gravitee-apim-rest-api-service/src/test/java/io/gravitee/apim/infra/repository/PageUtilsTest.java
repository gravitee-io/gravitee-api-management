package io.gravitee.apim.infra.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class PageUtilsTest {

    @Test
    @SneakyThrows
    void should_stream_single_page() {
        // Given
        var page = new Page<>(List.of("a", "b", "c"), 0, 3, 3);

        // When
        var stream = PageUtils.toStream(p -> page);

        // Then
        assertThat(stream).containsExactly("a", "b", "c");
    }

    @Test
    @SneakyThrows
    void should_stream_multiple_pages() {
        // Given
        var page1 = new Page<>(List.of("a", "b", "c"), 0, 3, 8);
        var page2 = new Page<>(List.of("d", "e", "f"), 1, 3, 8);
        var page3 = new Page<>(List.of("g", "h"), 2, 2, 8);

        // When
        var stream = PageUtils.toStream(p -> {
            if (p.pageNumber() == 0) {
                return page1;
            } else if (p.pageNumber() == 1) {
                return page2;
            } else if (p.pageNumber() == 2) {
                return page3;
            }
            return null;
        });

        // Then
        assertThat(stream).containsExactly("a", "b", "c", "d", "e", "f", "g", "h");
    }

    @Test
    @SneakyThrows
    void should_stream_empty_page() {
        // Given
        var page = new Page<>(List.of(), 0, 0, 0);

        // When
        var stream = PageUtils.toStream(p -> page);

        // Then
        assertThat(stream).isEmpty();
    }

    @Test
    @SneakyThrows
    void should_throw_exception_when_page_supplier_throws_exception_on_first_page() {
        // Given
        var exception = new TechnicalException("unexpected error");

        // When
        var throwable = catchThrowable(() ->
            PageUtils.toStream(p -> {
                throw exception;
            })
        );

        // Then
        assertThat(throwable).isInstanceOf(TechnicalException.class).hasMessage("unexpected error");
    }

    @Test
    @SneakyThrows
    void should_throw_exception_when_page_supplier_throws_exception_on_second_page() {
        // Given
        var page1 = new Page<>(List.of("a", "b", "c"), 0, 3, 6);
        var exception = new TechnicalException("unexpected error");

        // When
        var throwable = catchThrowable(() ->
            PageUtils
                .toStream(p -> {
                    if (p.pageNumber() == 0) {
                        return page1;
                    } else if (p.pageNumber() == 1) {
                        throw exception;
                    }
                    return null;
                })
                .toList()
        );

        // Then
        assertThat(throwable)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("io.gravitee.repository.exceptions.TechnicalException: unexpected error");
    }
}
