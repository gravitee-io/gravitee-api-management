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
package io.gravitee.apim.infra.domain_service.theme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.theme.exception.ThemeImageInvalidException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ThemeImageWriteValidatorImplTest {

    private final ThemeImageWriteValidatorImpl validator = new ThemeImageWriteValidatorImpl();

    @ParameterizedTest
    @CsvSource(
        {
            "png,image/png",
            "png,image/x-png",
            "jpeg,image/jpeg",
            "jpeg,image/jpg",
            "gif,image/gif",
            "bmp,image/bmp",
            "bmp,image/x-ms-bmp",
            "tiff,image/tiff",
            "tiff,image/x-tiff",
        }
    )
    void should_accept_real_passive_images_and_their_mime_aliases(String format, String mimeType) throws IOException {
        var value = dataUri(mimeType, imageBytes(format));

        assertThatNoException().isThrownBy(() -> validator.validate("logo", value));
    }

    @ParameterizedTest
    @ValueSource(strings = { "image/x-icon", "image/vnd.microsoft.icon", "image/ico" })
    void should_accept_real_ico_images(String mimeType) throws IOException {
        var png = imageBytes("png");
        var ico = ByteBuffer.allocate(22 + png.length).order(ByteOrder.LITTLE_ENDIAN);
        ico.putShort((short) 0).putShort((short) 1).putShort((short) 1);
        ico.put((byte) 1).put((byte) 1).put((byte) 0).put((byte) 0);
        ico.putShort((short) 1).putShort((short) 24).putInt(png.length).putInt(22).put(png);

        assertThatNoException().isThrownBy(() -> validator.validate("favicon", dataUri(mimeType, ico.array())));
    }

    @Test
    void should_accept_webp() {
        var webp = Base64.getDecoder().decode("UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA");

        assertThatNoException().isThrownBy(() -> validator.validate("backgroundImage", dataUri("image/webp", webp)));
    }

    @Test
    void should_accept_a_real_image_with_a_charset_parameter() throws IOException {
        var value = dataUri("image/png;charset=UTF-8", imageBytes("png"));

        assertThatNoException().isThrownBy(() -> validator.validate("logo", value));
    }

    @Test
    void should_accept_case_insensitive_data_uri_metadata() throws IOException {
        var value = dataUri("IMAGE/X-PNG", imageBytes("png")).replace("data:", "DATA:").replace(";base64,", ";BASE64,");

        assertThatNoException().isThrownBy(() -> validator.validate("logo", value));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "https://example.test/logo.png", "http://example.test/favicon.ico" })
    void should_allow_absent_images_and_absolute_http_urls_without_fetching(String value) {
        assertThatNoException().isThrownBy(() -> validator.validate("logo", value));
    }

    @ParameterizedTest
    @ValueSource(strings = { "<html><body>not an image</body></html>", "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>", "plain text" })
    void should_reject_content_masquerading_as_an_allowed_image(String content) {
        assertThatThrownBy(() -> validator.validate("favicon", dataUri("image/png", content.getBytes(StandardCharsets.UTF_8))))
            .isInstanceOf(ThemeImageInvalidException.class)
            .hasMessageContaining("favicon")
            .hasMessageContaining("does not match")
            .hasMessageNotContaining(content);
    }

    @Test
    void should_reject_mismatched_passive_image_types() throws IOException {
        var mislabeled = dataUri("image/jpeg", imageBytes("png"));

        assertThatThrownBy(() -> validator.validate("optionalLogo", mislabeled))
            .isInstanceOf(ThemeImageInvalidException.class)
            .hasMessageContaining("does not match");
    }

    @ParameterizedTest
    @ValueSource(strings = { "image/svg+xml", "text/html", "application/xhtml+xml", "application/xml", "image/unknown" })
    void should_reject_unsupported_declared_types_even_for_passive_content(String mimeType) throws IOException {
        var value = dataUri(mimeType, imageBytes("png"));

        assertThatThrownBy(() -> validator.validate("logo", value)).isInstanceOf(ThemeImageInvalidException.class);
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "logo.png",
            "javascript:alert(1)",
            "ftp://example.test/logo.png",
            "https:/example.test/logo.png",
            " ",
            "data:image/png;base64,invalid!",
        }
    )
    void should_reject_malformed_images_and_unsupported_urls(String value) {
        assertThatThrownBy(() -> validator.validate("logo", value)).isInstanceOf(ThemeImageInvalidException.class);
    }

    private static byte[] imageBytes(String format) throws IOException {
        var image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        var output = new ByteArrayOutputStream();
        assertThat(ImageIO.write(image, format, output)).isTrue();
        return output.toByteArray();
    }

    private static String dataUri(String mimeType, byte[] content) {
        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(content);
    }
}
