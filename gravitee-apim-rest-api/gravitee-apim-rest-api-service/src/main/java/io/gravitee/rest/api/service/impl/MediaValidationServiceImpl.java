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
package io.gravitee.rest.api.service.impl;

import io.gravitee.rest.api.model.MediaEntity;
import io.gravitee.rest.api.service.MediaValidationService;
import io.gravitee.rest.api.service.exceptions.UploadUnauthorized;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.common.PDDestinationOrAction;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionImportData;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionLaunch;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionSubmitForm;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MediaValidationServiceImpl implements MediaValidationService {

    private static final Tika TIKA = new Tika();

    private static final Pattern MEDIA_TYPE_PATTERN = Pattern.compile("^(?<major>[^/]*(application|image|audio|video)[^/]*)");

    private static final Set<String> FORBIDDEN_TYPES = Set.of(
        "application/java-archive",
        "application/x-dosexec",
        "application/x-msdownload",
        "application/x-ms-installer",
        "application/x-shockwave-flash",
        "application/x-bsh",
        "application/x-sh",
        "application/x-csh",
        "application/javascript",
        "application/vbscript",
        "application/sql",
        "application/x-executable",
        "application/bat",
        "application/x-bat",
        "application/octet-stream",
        "text/x-script.sh"
    );

    public void validate(MediaEntity mediaEntity) {
        if (mediaEntity == null || mediaEntity.getData() == null) {
            log.debug("Validation skipped: mediaEntity or data is null");
            return;
        }

        if (mediaEntity.getType() == null || mediaEntity.getSubType() == null) {
            log.warn("Validation failed for file '{}': missing media type", mediaEntity.getFileName());
            throw new UploadUnauthorized("The file should have media type.");
        }

        String mediaType = mediaEntity.getType() + "/" + mediaEntity.getSubType();

        String detectedMediaType;
        try {
            detectedMediaType = TIKA.detect(mediaEntity.getData(), mediaEntity.getFileName());
        } catch (IllegalStateException e) {
            log.error("Unable to determine file type for '{}'", mediaEntity.getFileName(), e);
            throw new UploadUnauthorized("Unable to determine file type.");
        }

        log.debug("File '{}': declared type='{}', detected type='{}'", mediaEntity.getFileName(), mediaType, detectedMediaType);

        checkContentTypeMismatch(mediaType, detectedMediaType, mediaEntity.getFileName());

        (mediaType.equals(detectedMediaType) ? Set.of(mediaType) : Set.of(mediaType, detectedMediaType)).forEach(type -> {
            if (isForbidden(type)) {
                log.warn("Forbidden file type '{}' detected for file '{}'", type, mediaEntity.getFileName());
                throw new UploadUnauthorized("Unsupported file type: " + type + ". Uploading this type of file is not allowed.");
            }
        });

        if ("application/pdf".equals(mediaType) || "application/pdf".equals(detectedMediaType)) {
            if (!isAcceptablePDF(mediaEntity)) throw new UploadUnauthorized("This pdf use unallowed features.");
        }
    }

    private boolean isForbidden(String mediaType) {
        if (mediaType == null) {
            return true;
        }

        return FORBIDDEN_TYPES.contains(mediaType.toLowerCase());
    }

    private boolean isAcceptablePDF(MediaEntity media) {
        try (PDDocument document = Loader.loadPDF(media.getData())) {
            if (document.getNumberOfPages() == 0) {
                log.warn("No pages found for PDF file {}.", media.getFileName());
                return false;
            }

            if (document.isEncrypted()) {
                log.warn("PDF file {} should not be encrypted.", media.getFileName());
                return false;
            }

            PDDocumentCatalog catalog = document.getDocumentCatalog();
            if (catalog.getNames() != null && catalog.getNames().getJavaScript() != null) {
                log.warn("PDF file {} should not use Javascript.", media.getFileName());
                return false;
            }

            PDDestinationOrAction openAction = catalog.getOpenAction();
            boolean isForbiddenAction = (openAction instanceof PDActionJavaScript ||
                openAction instanceof PDActionLaunch ||
                openAction instanceof PDActionImportData ||
                openAction instanceof PDActionSubmitForm);
            if (isForbiddenAction) {
                log.warn("PDF file {} use forbidden open action.", media.getFileName());
                return false;
            }

            return true;
        } catch (IOException e) {
            log.error("Unable to read the PDF file {}.", media.getFileName(), e);
            return false;
        }
    }

    private void checkContentTypeMismatch(String mediaType, String detectedMediaType, String fileName) {
        if (detectedMediaType == null) {
            return;
        }

        var match = MEDIA_TYPE_PATTERN.matcher(mediaType);
        if (match.find() && !detectedMediaType.startsWith(match.group("major") + '/')) {
            throw new UploadUnauthorized(
                String.format(
                    "File content does not match its extension. Declared as '%s/...', but content is of type '%s'.",
                    match.group("major"),
                    detectedMediaType
                )
            );
        }
    }
}
