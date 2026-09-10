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

import io.gravitee.apim.core.theme.domain_service.ThemePortalNextAssetsDomainService;
import jakarta.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@CustomLog
public class ThemePortalNextAssetsDomainServiceImpl implements ThemePortalNextAssetsDomainService {

    private static final String DEFAULT_CUSTOM_CSS_TEMPLATE = "templates/default-portal-next-custom-css.css";
    private static final String DEFAULT_LOGO_TEMPLATE = "templates/default-portal-next-logo.png";

    private final String themeNextPath;
    private String defaultCustomCss;
    private String defaultLogo;

    public ThemePortalNextAssetsDomainServiceImpl(@Value("${portal.themes.path:${gravitee.home}/themes}/next") String themeNextPath) {
        this.themeNextPath = themeNextPath;
    }

    @Override
    public String getPortalNextLogo() {
        String logo = getImage("logo.png");
        if (logo != null) {
            return logo;
        }
        return getClasspathLogo();
    }

    @Override
    public String getPortalNextFavicon() {
        return getImage("favicon.png");
    }

    @Override
    public String getDefaultCustomCss() {
        if (defaultCustomCss == null) {
            try {
                var resource = new ClassPathResource(DEFAULT_CUSTOM_CSS_TEMPLATE);
                if (resource.exists()) {
                    defaultCustomCss = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                } else {
                    defaultCustomCss = "";
                }
            } catch (IOException e) {
                log.warn("Could not load default Portal Next custom CSS template", e);
                defaultCustomCss = "";
            }
        }
        return defaultCustomCss.isEmpty() ? null : defaultCustomCss;
    }

    private String getClasspathLogo() {
        if (defaultLogo == null) {
            defaultLogo = encodeClasspathImage(DEFAULT_LOGO_TEMPLATE, "logo.png");
        }
        return defaultLogo.isEmpty() ? null : defaultLogo;
    }

    private String encodeClasspathImage(String resourcePath, String filename) {
        try {
            var resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                return "";
            }
            byte[] image = resource.getInputStream().readAllBytes();
            return encodeImage(image, filename);
        } catch (IOException e) {
            log.warn("Could not load default Portal Next image from classpath: {}", resourcePath, e);
            return "";
        }
    }

    private String getImage(String filename) {
        String filepath = this.themeNextPath + "/" + filename;
        File imageFile = new File(filepath);
        if (!imageFile.exists()) {
            return null;
        }
        try {
            byte[] image = Files.readAllBytes(imageFile.toPath());
            return encodeImage(image, filename);
        } catch (IOException ex) {
            final String error = "Error while trying to load image from: " + filepath;
            log.error(error, ex);
            return null;
        }
    }

    private String encodeImage(byte[] image, String filename) {
        MimetypesFileTypeMap fileTypeMap = new MimetypesFileTypeMap();
        return "data:" + fileTypeMap.getContentType(filename) + ";base64," + Base64.getEncoder().encodeToString(image);
    }
}
