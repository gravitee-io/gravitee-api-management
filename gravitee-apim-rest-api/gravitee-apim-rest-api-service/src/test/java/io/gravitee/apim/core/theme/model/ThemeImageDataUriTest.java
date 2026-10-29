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
package io.gravitee.apim.core.theme.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.theme.exception.ThemeImageInvalidException;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ThemeImageDataUriTest {

    @Test
    void should_compare_independently_decoded_images_by_content() {
        var image = ThemeImageDataUri.parse("logo", "data:image/png;base64,AQID");
        var sameImage = ThemeImageDataUri.parse("logo", "data:image/png;base64,AQID");

        assertThat(image.bytes()).isNotSameAs(sameImage.bytes());
        assertThat(image).isEqualTo(sameImage);
        assertThat(sameImage).isEqualTo(image);
    }

    @Test
    void should_produce_the_same_hash_for_equal_image_content() {
        var image = ThemeImageDataUri.parse("logo", "data:image/png;base64,AQID");
        var sameImage = ThemeImageDataUri.parse("logo", "data:image/png;base64,AQID");

        assertThat(image.hashCode()).isEqualTo(sameImage.hashCode());
    }

    @ParameterizedTest
    @ValueSource(strings = { "data:image/jpeg;base64,AQID", "data:image/png;base64,AQIE" })
    void should_distinguish_images_with_different_mime_types_or_content(String value) {
        var image = ThemeImageDataUri.parse("logo", "data:image/png;base64,AQID");
        var differentImage = ThemeImageDataUri.parse("logo", value);

        assertThat(image).isNotEqualTo(differentImage);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "not-an-image" })
    void should_not_equal_other_objects(Object other) {
        var image = ThemeImageDataUri.parse("logo", "data:image/png;base64,AQID");

        assertThat(image).isNotEqualTo(other);
    }

    @Test
    void should_preserve_null_component_support() {
        var image = new ThemeImageDataUri(null, null);
        var sameImage = new ThemeImageDataUri(null, null);

        assertThat(image).isEqualTo(sameImage);
        assertThat(image.hashCode()).isEqualTo(sameImage.hashCode());
        assertThat(image.toString()).isEqualTo("ThemeImageDataUri[mimeType=null, byteCount=null]");
    }

    @Test
    void should_describe_image_metadata_without_exposing_content() {
        var image = ThemeImageDataUri.parse("logo", "data:image/png;base64,AQID");

        assertThat(image.toString()).isEqualTo("ThemeImageDataUri[mimeType=image/png, byteCount=3]");
    }

    @Test
    void should_keep_the_description_compact_for_a_maximum_size_image() {
        var image = new ThemeImageDataUri("image/png", new byte[ThemeImageDataUri.MAX_DECODED_BYTES]);

        assertThat(image.toString()).isEqualTo("ThemeImageDataUri[mimeType=image/png, byteCount=5242880]");
    }

    @ParameterizedTest
    @CsvSource(
        {
            "image/png,image/png",
            "IMAGE/X-PNG,image/png",
            "image/jpg,image/jpeg",
            "image/pjpeg,image/jpeg",
            "image/x-icon,image/vnd.microsoft.icon",
            "image/gif,image/gif",
            "image/webp,image/webp",
            "image/x-ms-bmp,image/bmp",
            "image/x-tiff,image/tiff",
        }
    )
    void should_decode_allowed_types_using_canonical_mime_without_detecting_content(String declared, String canonical) {
        var image = ThemeImageDataUri.parse("favicon", "DATA:" + declared + ";BASE64,AQID");

        assertThat(image.mimeType()).isEqualTo(canonical);
        assertThat(image.bytes()).containsExactly(1, 2, 3);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
        strings = {
            "not-an-image",
            "data:image/png,AQID",
            "data:image/png;base64",
            "data:;base64,AQID",
            "data:image/png;charset=;base64,AQID",
            "data:image/png;=UTF-8;base64,AQID",
            "data:image/png;;base64,AQID",
            "data:image/png;charset=UTF 8;base64,AQID",
            "data:image/png;base64,",
            "data:image/png;base64,not base64",
            "data:image/png;base64,AQI===",
            "data:image/png;base64,AQ ID",
            "data:text/html;base64,AQID",
            "data:application/xhtml+xml;base64,AQID",
            "data:image/svg+xml;base64,AQID",
            "data:image/unknown;base64,AQID",
        }
    )
    void should_reject_invalid_data_uris_with_safe_field_specific_errors(String value) {
        assertThatThrownBy(() -> ThemeImageDataUri.parse("favicon", value))
            .isInstanceOf(ThemeImageInvalidException.class)
            .hasMessageStartingWith("Invalid theme image in favicon:");
    }

    @Test
    void should_ignore_well_formed_media_parameters_when_selecting_the_canonical_mime() {
        var image = ThemeImageDataUri.parse("logo", "data:image/x-png;charset=UTF-8;name=logo.png;base64,AQID");

        assertThat(image.mimeType()).isEqualTo("image/png");
        assertThat(image.bytes()).containsExactly(1, 2, 3);
    }

    @Test
    void should_reject_an_overlong_header() {
        assertThatThrownBy(() -> ThemeImageDataUri.parse("logo", "data:" + "a".repeat(128) + ";base64,AQID")).isInstanceOf(
            ThemeImageInvalidException.class
        );
    }

    @Test
    void should_reject_excessive_encoded_content_before_decoding() {
        var excessiveEncoded = "A".repeat(((ThemeImageDataUri.MAX_DECODED_BYTES + 2) / 3) * 4 + 4);

        assertThatThrownBy(() -> ThemeImageDataUri.parse("logo", "data:image/png;base64," + excessiveEncoded))
            .isInstanceOf(ThemeImageInvalidException.class)
            .hasMessageContaining("5 MiB")
            .hasNoCause();
    }

    @Test
    void should_accept_exactly_the_decoded_limit() {
        var content = new byte[ThemeImageDataUri.MAX_DECODED_BYTES];

        var result = ThemeImageDataUri.parse("logo", "data:image/png;base64," + Base64.getEncoder().encodeToString(content));

        assertThat(result.bytes()).hasSize(ThemeImageDataUri.MAX_DECODED_BYTES);
    }

    @Test
    void should_check_decoded_size_even_when_encoded_length_fits() {
        var content = new byte[ThemeImageDataUri.MAX_DECODED_BYTES + 1];

        assertThatThrownBy(() -> ThemeImageDataUri.parse("logo", "data:image/png;base64," + Base64.getEncoder().encodeToString(content)))
            .isInstanceOf(ThemeImageInvalidException.class)
            .hasMessageContaining("5 MiB");
    }

    @Test
    void should_not_include_invalid_content_in_error_message() {
        assertThatThrownBy(() -> ThemeImageDataUri.parse("logo", "data:image/png;base64,private-content!"))
            .isInstanceOf(ThemeImageInvalidException.class)
            .hasMessageNotContaining("private-content")
            .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "https://example.test/logo.png",
            "HTTP://example.test/favicon.ico",
            "https://example.test/a?b=1#c",
            "http://localhost:8083/logo",
        }
    )
    void should_accept_absolute_http_urls(String value) {
        assertThat(ThemeImageDataUri.isHttpUrl(value)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
        strings = {
            "logo.png",
            "/logo.png",
            "//example.test/logo",
            "https:/example.test/logo",
            "https://",
            "javascript:alert(1)",
            "data:image/png;base64,AQID",
            "https://example.test/a b",
        }
    )
    void should_reject_non_http_or_malformed_urls(String value) {
        assertThat(ThemeImageDataUri.isHttpUrl(value)).isFalse();
    }
}
