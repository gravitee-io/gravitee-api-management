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
package io.gravitee.rest.api.portal.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import inmemory.ThemeQueryServiceInMemory;
import io.gravitee.apim.core.theme.exception.ThemeImageInvalidException;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.PictureEntity;
import io.gravitee.rest.api.model.UrlPictureEntity;
import io.gravitee.rest.api.portal.rest.model.ThemeResponse;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.Base64;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author GraviteeSource Team
 */
public class ThemeResourceTest extends AbstractResourceTest {

    private static final String THEME_ID = "my-theme-id";

    @Autowired
    private ThemeQueryServiceInMemory themeQueryService;

    @Override
    protected String contextPath() {
        return "theme";
    }

    @BeforeEach
    public void init() {
        when(themeMapper.convert(any(Theme.class), anyString())).thenCallRealMethod();
        reset(themeService);
    }

    @AfterEach
    public void destroy() {
        this.themeQueryService.reset();
    }

    @Test
    public void shouldGetDefaultPortalTheme() {
        final Response response = target().queryParam("type", "PORTAL").request().get();

        ThemeResponse resultTheme = response.readEntity(ThemeResponse.class);
        assertNotNull(resultTheme);
        assertEquals(ThemeResponse.TypeEnum.PORTAL, resultTheme.getType());
    }

    @Test
    public void shouldGetDefaultPortalNextTheme() {
        final Response response = target().queryParam("type", "PORTAL_NEXT").request().get();

        ThemeResponse resultTheme = response.readEntity(ThemeResponse.class);
        assertNotNull(resultTheme);
        assertEquals(ThemeResponse.TypeEnum.PORTAL_NEXT, resultTheme.getType());
    }

    @Test
    public void should_have_error_400_if_no_query() {
        final Response response = target().request().get();
        assertEquals(400, response.getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = { "logo", "optionalLogo", "backgroundImage", "favicon" })
    void should_not_serve_invalid_theme_image(String asset) {
        image(asset).thenThrow(new ThemeImageInvalidException(asset, "Unsupported image format"));

        final Response response = target("/" + THEME_ID + "/" + asset).request().get();

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.hasEntity()).isFalse();
        assertThat(response.getHeaderString(HttpHeaders.CACHE_CONTROL)).contains("no-store");
        assertThat(response.getEntityTag()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = { "logo", "optionalLogo", "backgroundImage", "favicon" })
    void should_not_return_cached_invalid_theme_image(String asset) {
        image(asset).thenThrow(new ThemeImageInvalidException(asset, "Unsupported image format"));

        final Response response = target("/" + THEME_ID + "/" + asset).request().header(HttpHeaders.IF_NONE_MATCH, "*").get();

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.hasEntity()).isFalse();
        assertThat(response.getHeaderString(HttpHeaders.CACHE_CONTROL)).contains("no-store");
    }

    @ParameterizedTest
    @ValueSource(strings = { "logo", "optionalLogo", "backgroundImage", "favicon" })
    void should_preserve_theme_image_caching(String asset) {
        var picture = inlinePicture();
        image(asset).thenReturn(picture);

        final Response response = target("/" + THEME_ID + "/" + asset).request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getMediaType().toString()).isEqualTo("image/png");
        assertThat(response.readEntity(byte[].class)).isEqualTo(picture.getContent());
        assertThat(response.getHeaderString(HttpHeaders.CACHE_CONTROL)).contains("max-age=86400", "no-transform");
        assertThat(response.getEntityTag()).isNotNull();

        final Response cached = target("/" + THEME_ID + "/" + asset)
            .request()
            .header(HttpHeaders.IF_NONE_MATCH, response.getEntityTag())
            .get();

        assertThat(cached.getStatus()).isEqualTo(304);
        assertThat(cached.hasEntity()).isFalse();
        assertThat(cached.getHeaderString(HttpHeaders.CACHE_CONTROL)).isEqualTo(response.getHeaderString(HttpHeaders.CACHE_CONTROL));
    }

    @ParameterizedTest
    @ValueSource(strings = { "logo", "optionalLogo", "backgroundImage", "favicon" })
    void should_preserve_external_theme_image_redirect(String asset) {
        image(asset).thenReturn(new UrlPictureEntity("https://images.example.com/theme.png"));

        final Response response = target("/" + THEME_ID + "/" + asset).property(ClientProperties.FOLLOW_REDIRECTS, false).request().get();

        assertThat(response.getStatus()).isEqualTo(307);
        assertThat(response.getLocation().toString()).isEqualTo("https://images.example.com/theme.png");
    }

    @ParameterizedTest
    @ValueSource(strings = { "logo", "optionalLogo", "backgroundImage", "favicon" })
    void should_preserve_empty_theme_image_response(String asset) {
        image(asset).thenReturn(null);

        final Response response = target("/" + THEME_ID + "/" + asset).request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.hasEntity()).isFalse();
    }

    @Test
    void should_fall_back_to_logo_when_optional_logo_is_absent() {
        var picture = inlinePicture();
        image("optionalLogo").thenReturn(null);
        image("logo").thenReturn(picture);

        final Response response = target("/" + THEME_ID + "/optionalLogo").request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.readEntity(byte[].class)).isEqualTo(picture.getContent());
    }

    @Test
    void should_not_serve_invalid_fallback_logo() {
        image("optionalLogo").thenReturn(null);
        image("logo").thenThrow(new ThemeImageInvalidException("logo", "Unsupported image format"));

        final Response response = target("/" + THEME_ID + "/optionalLogo").request().header(HttpHeaders.IF_NONE_MATCH, "*").get();

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.hasEntity()).isFalse();
        assertThat(response.getHeaderString(HttpHeaders.CACHE_CONTROL)).contains("no-store");
    }

    private OngoingStubbing<PictureEntity> image(String asset) {
        return switch (asset) {
            case "logo" -> when(themeService.getLogo(any(), anyString()));
            case "optionalLogo" -> when(themeService.getOptionalLogo(any(), anyString()));
            case "backgroundImage" -> when(themeService.getBackgroundImage(any(), anyString()));
            case "favicon" -> when(themeService.getFavicon(any(), anyString()));
            default -> throw new IllegalArgumentException("Unknown theme image: " + asset);
        };
    }

    private InlinePictureEntity inlinePicture() {
        var picture = new InlinePictureEntity();
        picture.setType("image/png");
        picture.setContent(
            Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=")
        );
        return picture;
    }
}
