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

import io.gravitee.apim.core.theme.domain_service.ThemeImageWriteValidator;
import io.gravitee.apim.core.theme.exception.ThemeImageInvalidException;
import io.gravitee.apim.core.theme.model.ThemeImageDataUri;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

@Service
public class ThemeImageWriteValidatorImpl implements ThemeImageWriteValidator {

    private static final Tika TIKA = new Tika();

    @Override
    public void validate(String field, String value) {
        if (value == null || value.isEmpty() || ThemeImageDataUri.isHttpUrl(value)) {
            return;
        }
        var image = ThemeImageDataUri.parse(field, value);
        String detected = TIKA.detect(image.bytes());
        String detectedMimeType = ThemeImageDataUri.canonicalMimeType(
            "image/bmp;format=compressed".equals(detected) ? "image/bmp" : detected
        );
        if (!image.mimeType().equals(detectedMimeType)) {
            throw new ThemeImageInvalidException(field, "image content does not match its declared media type");
        }
    }
}
