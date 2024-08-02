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
import java.nio.file.Files;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ThemePortalNextAssetsDomainServiceImpl implements ThemePortalNextAssetsDomainService {

    private final String themeNextPath;

    public ThemePortalNextAssetsDomainServiceImpl(@Value("${portal.themes.path:${gravitee.home}/themes}/next") String themeNextPath) {
        this.themeNextPath = themeNextPath;
    }

    @Override
    public String getPortalNextLogo() {
        return getImage("logo.png");
    }

    @Override
    public String getPortalNextFavicon() {
        return getImage("favicon.png");
    }

    private String getImage(String filename) {
        String filepath = this.themeNextPath + "/" + filename;
        File imageFile = new File(filepath);
        if (!imageFile.exists()) {
            return null;
        }
        try {
            byte[] image = Files.readAllBytes(imageFile.toPath());
            MimetypesFileTypeMap fileTypeMap = new MimetypesFileTypeMap();
            return "data:" + fileTypeMap.getContentType(filename) + ";base64," + Base64.getEncoder().encodeToString(image);
        } catch (IOException ex) {
            final String error = "Error while trying to load image from: " + filepath;
            log.error(error, ex);
            return null;
        }
    }
}
