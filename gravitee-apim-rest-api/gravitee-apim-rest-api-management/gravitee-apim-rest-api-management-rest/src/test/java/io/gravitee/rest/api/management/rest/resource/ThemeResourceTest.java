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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.*;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.theme.exception.ThemeImageInvalidException;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.PictureEntity;
import io.gravitee.rest.api.model.UrlPictureEntity;
import io.gravitee.rest.api.model.theme.portal.ThemeDefinition;
import io.gravitee.rest.api.model.theme.portal.ThemeEntity;
import io.gravitee.rest.api.model.theme.portal.UpdateThemeEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ThemeNotFoundException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.Base64;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.stubbing.OngoingStubbing;

/**
 * @author GraviteeSource Team
 */
public class ThemeResourceTest extends AbstractResourceTest {

    private static final String THEME_ID = "my-theme-id";

    @Override
    protected String contextPath() {
        return "configuration/themes/";
    }

    @BeforeEach
    public void init() {
        reset(themeService);
    }

    @Test
    public void shouldNotFindThemeById() {
        when(themeService.findById(GraviteeContext.getExecutionContext(), THEME_ID)).thenThrow(ThemeNotFoundException.class);

        final Response response = envTarget(THEME_ID).request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldFindThemeById() {
        ThemeEntity theme = new ThemeEntity();
        theme.setId(THEME_ID);

        when(themeService.findById(GraviteeContext.getExecutionContext(), THEME_ID)).thenReturn(theme);

        final Response response = envTarget(THEME_ID).request().get();

        assertEquals(OK_200, response.getStatus());
        final ThemeEntity responseTheme = response.readEntity(ThemeEntity.class);
        assertNotNull(responseTheme);
        assertEquals(theme, responseTheme);
    }

    @Test
    public void shouldDeleteTheme() {
        ThemeEntity theme = new ThemeEntity();
        theme.setId(THEME_ID);

        when(themeService.findById(GraviteeContext.getExecutionContext(), THEME_ID)).thenReturn(theme);
        final Response response = envTarget(THEME_ID).request().delete();

        assertEquals(NO_CONTENT_204, response.getStatus());
        verify(themeService).delete(GraviteeContext.getExecutionContext(), THEME_ID);
    }

    @Test
    public void shouldUpdateTheme() {
        ThemeEntity oldTheme = new ThemeEntity();
        oldTheme.setId(THEME_ID);

        when(themeService.findById(GraviteeContext.getExecutionContext(), THEME_ID)).thenReturn(oldTheme);

        UpdateThemeEntity updateTheme = new UpdateThemeEntity();
        updateTheme.setId(THEME_ID);
        updateTheme.setName("my-theme");
        updateTheme.setDefinition(new ThemeDefinition());

        ThemeEntity theme = new ThemeEntity();
        theme.setId(THEME_ID);

        when(themeService.updatePortalTheme(GraviteeContext.getExecutionContext(), updateTheme)).thenReturn(theme);

        final Response response = envTarget(THEME_ID).request().put(json(updateTheme));

        assertEquals(OK_200, response.getStatus());
        verify(themeService).updatePortalTheme(eq(GraviteeContext.getExecutionContext()), eq(updateTheme));

        final ThemeEntity responseTheme = response.readEntity(ThemeEntity.class);
        assertNotNull(responseTheme);
        assertEquals(theme, responseTheme);
    }

    @Test
    public void shouldNotFindPortalNextThemeById() {
        var theme = io.gravitee.rest.api.model.theme.portalnext.ThemeEntity.builder().id(THEME_ID).build();

        when(themeService.findById(GraviteeContext.getExecutionContext(), THEME_ID)).thenReturn(theme);
        final Response response = envTarget(THEME_ID).request().get();

        assertEquals(BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldNotDeletePortalNextTheme() {
        var theme = io.gravitee.rest.api.model.theme.portalnext.ThemeEntity.builder().id(THEME_ID).build();

        when(themeService.findById(GraviteeContext.getExecutionContext(), THEME_ID)).thenReturn(theme);
        final Response response = envTarget(THEME_ID).request().delete();

        assertEquals(BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldNotUpdatePortalNextTheme() {
        var oldTheme = io.gravitee.rest.api.model.theme.portalnext.ThemeEntity.builder().id(THEME_ID).build();

        when(themeService.findById(GraviteeContext.getExecutionContext(), THEME_ID)).thenReturn(oldTheme);
        UpdateThemeEntity updateTheme = new UpdateThemeEntity();
        updateTheme.setId(THEME_ID);
        updateTheme.setName("my-theme");
        updateTheme.setDefinition(new ThemeDefinition());

        ThemeEntity theme = new ThemeEntity();
        theme.setId(THEME_ID);

        when(themeService.updatePortalTheme(GraviteeContext.getExecutionContext(), updateTheme)).thenReturn(theme);

        final Response response = envTarget(THEME_ID).request().put(json(updateTheme));

        assertEquals(BAD_REQUEST_400, response.getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = { "logo", "optionalLogo", "backgroundImage", "favicon" })
    void should_not_serve_invalid_theme_image(String asset) {
        image(asset).thenThrow(new ThemeImageInvalidException(asset, "Unsupported image format"));

        final Response response = envTarget(THEME_ID + "/" + asset).request().get();

        assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
        assertThat(response.hasEntity()).isFalse();
        assertThat(response.getHeaderString(HttpHeaders.CACHE_CONTROL)).contains("no-store");
        assertThat(response.getEntityTag()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = { "logo", "optionalLogo", "backgroundImage", "favicon" })
    void should_not_return_cached_invalid_theme_image(String asset) {
        image(asset).thenThrow(new ThemeImageInvalidException(asset, "Unsupported image format"));

        final Response response = envTarget(THEME_ID + "/" + asset).request().header(HttpHeaders.IF_NONE_MATCH, "*").get();

        assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
        assertThat(response.hasEntity()).isFalse();
        assertThat(response.getHeaderString(HttpHeaders.CACHE_CONTROL)).contains("no-store");
    }

    @ParameterizedTest
    @ValueSource(strings = { "logo", "optionalLogo", "backgroundImage", "favicon" })
    void should_preserve_theme_image_caching(String asset) {
        var picture = inlinePicture();
        image(asset).thenReturn(picture);

        final Response response = envTarget(THEME_ID + "/" + asset).request().get();

        assertThat(response.getStatus()).isEqualTo(OK_200);
        assertThat(response.getMediaType().toString()).isEqualTo("image/png");
        assertThat(response.readEntity(byte[].class)).isEqualTo(picture.getContent());
        assertThat(response.getHeaderString(HttpHeaders.CACHE_CONTROL)).contains("max-age=86400", "no-transform");
        assertThat(response.getEntityTag()).isNotNull();

        final Response cached = envTarget(THEME_ID + "/" + asset)
            .request()
            .header(HttpHeaders.IF_NONE_MATCH, response.getEntityTag())
            .get();

        assertThat(cached.getStatus()).isEqualTo(NOT_MODIFIED_304);
        assertThat(cached.hasEntity()).isFalse();
        assertThat(cached.getHeaderString(HttpHeaders.CACHE_CONTROL)).isEqualTo(response.getHeaderString(HttpHeaders.CACHE_CONTROL));
    }

    @ParameterizedTest
    @ValueSource(strings = { "logo", "optionalLogo", "backgroundImage", "favicon" })
    void should_preserve_external_theme_image_redirect(String asset) {
        image(asset).thenReturn(new UrlPictureEntity("https://images.example.com/theme.png"));

        final Response response = envTarget(THEME_ID + "/" + asset).property(ClientProperties.FOLLOW_REDIRECTS, false).request().get();

        assertThat(response.getStatus()).isEqualTo(TEMPORARY_REDIRECT_307);
        assertThat(response.getLocation().toString()).isEqualTo("https://images.example.com/theme.png");
    }

    @ParameterizedTest
    @ValueSource(strings = { "logo", "optionalLogo", "backgroundImage", "favicon" })
    void should_preserve_empty_theme_image_response(String asset) {
        image(asset).thenReturn(null);

        final Response response = envTarget(THEME_ID + "/" + asset).request().get();

        assertThat(response.getStatus()).isEqualTo(OK_200);
        assertThat(response.hasEntity()).isFalse();
    }

    private OngoingStubbing<PictureEntity> image(String asset) {
        var theme = new ThemeEntity();
        theme.setId(THEME_ID);
        when(themeService.findById(any(), eq(THEME_ID))).thenReturn(theme);

        return switch (asset) {
            case "logo" -> when(themeService.getLogo(any(), eq(THEME_ID)));
            case "optionalLogo" -> when(themeService.getOptionalLogo(any(), eq(THEME_ID)));
            case "backgroundImage" -> when(themeService.getBackgroundImage(any(), eq(THEME_ID)));
            case "favicon" -> when(themeService.getFavicon(any(), eq(THEME_ID)));
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
