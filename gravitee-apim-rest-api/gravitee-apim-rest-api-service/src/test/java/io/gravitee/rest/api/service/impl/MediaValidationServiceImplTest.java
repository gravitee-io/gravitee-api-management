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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.MediaEntity;
import io.gravitee.rest.api.service.exceptions.UploadUnauthorized;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionImportData;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionLaunch;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionSubmitForm;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination;
import org.apache.tika.Tika;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class MediaValidationServiceImplTest {

    private MediaValidationServiceImpl mediaValidationService;

    private MockedStatic<Loader> loader;
    private MockedConstruction<Tika> tikaConstruction;

    @BeforeEach
    public void setUp() {
        loader = Mockito.mockStatic(Loader.class);
        tikaConstruction = Mockito.mockConstruction(Tika.class, (mock, context) -> {
            when(mock.detect(Mockito.any(byte[].class), Mockito.nullable(String.class))).thenReturn("application/pdf");
        });
        mediaValidationService = new MediaValidationServiceImpl();
    }

    @AfterEach
    public void tearDown() {
        loader.close();
        tikaConstruction.close();
    }

    @Test
    void should_throw_when_pdf_has_no_pages() throws IOException {
        PDDocument document = mock(PDDocument.class);
        when(document.getNumberOfPages()).thenReturn(0);

        final byte[] data = "data".getBytes();
        loader.when(() -> Loader.loadPDF(data)).thenReturn(document);

        final MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setType("application");
        mediaEntity.setSubType("pdf");
        mediaEntity.setData(data);

        assertThrows(UploadUnauthorized.class, () -> mediaValidationService.validate(mediaEntity));
    }

    @Test
    void should_throw_when_pdf_is_encrypted() throws IOException {
        PDDocument document = mock(PDDocument.class);
        when(document.getNumberOfPages()).thenReturn(1);
        when(document.isEncrypted()).thenReturn(true);

        final byte[] data = "data".getBytes();
        loader.when(() -> Loader.loadPDF(data)).thenReturn(document);

        final MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setType("application");
        mediaEntity.setSubType("pdf");
        mediaEntity.setData(data);

        assertThrows(UploadUnauthorized.class, () -> mediaValidationService.validate(mediaEntity));
    }

    @Test
    void should_throw_when_pdf_has_javascript() throws IOException {
        PDDocument document = mock(PDDocument.class);
        when(document.getNumberOfPages()).thenReturn(1);
        PDDocumentCatalog catalog = mock(PDDocumentCatalog.class);
        when(document.getDocumentCatalog()).thenReturn(catalog);
        when(catalog.getOpenAction()).thenReturn(mock(PDActionJavaScript.class));

        final byte[] data = "data".getBytes();
        loader.when(() -> Loader.loadPDF(data)).thenReturn(document);

        final MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setType("application");
        mediaEntity.setSubType("pdf");
        mediaEntity.setData(data);

        assertThrows(UploadUnauthorized.class, () -> mediaValidationService.validate(mediaEntity));
    }

    @Test
    void should_throw_when_pdf_has_launch_action() throws IOException {
        PDDocument document = mock(PDDocument.class);
        when(document.getNumberOfPages()).thenReturn(1);
        PDDocumentCatalog catalog = mock(PDDocumentCatalog.class);
        when(document.getDocumentCatalog()).thenReturn(catalog);
        when(catalog.getOpenAction()).thenReturn(mock(PDActionLaunch.class));

        final byte[] data = "data".getBytes();
        loader.when(() -> Loader.loadPDF(data)).thenReturn(document);

        final MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setType("application");
        mediaEntity.setSubType("pdf");
        mediaEntity.setData(data);

        assertThrows(UploadUnauthorized.class, () -> mediaValidationService.validate(mediaEntity));
    }

    @Test
    void should_throw_when_pdf_has_import_data_action() throws IOException {
        PDDocument document = mock(PDDocument.class);
        when(document.getNumberOfPages()).thenReturn(1);
        PDDocumentCatalog catalog = mock(PDDocumentCatalog.class);
        when(document.getDocumentCatalog()).thenReturn(catalog);
        when(catalog.getOpenAction()).thenReturn(mock(PDActionImportData.class));

        final byte[] data = "data".getBytes();
        loader.when(() -> Loader.loadPDF(data)).thenReturn(document);

        final MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setType("application");
        mediaEntity.setSubType("pdf");
        mediaEntity.setData(data);

        assertThrows(UploadUnauthorized.class, () -> mediaValidationService.validate(mediaEntity));
    }

    @Test
    void should_throw_when_pdf_has_submit_form_action() throws IOException {
        PDDocument document = mock(PDDocument.class);
        when(document.getNumberOfPages()).thenReturn(1);
        PDDocumentCatalog catalog = mock(PDDocumentCatalog.class);
        when(document.getDocumentCatalog()).thenReturn(catalog);
        when(catalog.getOpenAction()).thenReturn(mock(PDActionSubmitForm.class));

        final byte[] data = "data".getBytes();
        loader.when(() -> Loader.loadPDF(data)).thenReturn(document);

        final MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setType("application");
        mediaEntity.setSubType("pdf");
        mediaEntity.setData(data);

        assertThrows(UploadUnauthorized.class, () -> mediaValidationService.validate(mediaEntity));
    }

    @Test
    void should_not_throw_when_pdf_is_valid() throws IOException {
        PDDocument document = mock(PDDocument.class);
        when(document.getNumberOfPages()).thenReturn(1);
        PDDocumentCatalog catalog = mock(PDDocumentCatalog.class);
        when(document.getDocumentCatalog()).thenReturn(catalog);
        when(catalog.getOpenAction()).thenReturn(mock(PDActionURI.class));

        final byte[] data = "data".getBytes();
        loader.when(() -> Loader.loadPDF(data)).thenReturn(document);

        final MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setType("application");
        mediaEntity.setSubType("pdf");
        mediaEntity.setData(data);

        assertDoesNotThrow(() -> mediaValidationService.validate(mediaEntity));
    }

    @Test
    void should_not_throw_when_pdf_is_valid_with_allowed_action() throws IOException {
        PDDocument document = mock(PDDocument.class);
        when(document.getNumberOfPages()).thenReturn(1);
        PDDocumentCatalog catalog = mock(PDDocumentCatalog.class);
        when(document.getDocumentCatalog()).thenReturn(catalog);
        when(catalog.getOpenAction()).thenReturn(mock(PDNamedDestination.class));

        final byte[] data = "data".getBytes();
        loader.when(() -> Loader.loadPDF(data)).thenReturn(document);

        final MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setType("application");
        mediaEntity.setSubType("pdf");
        mediaEntity.setData(data);

        assertDoesNotThrow(() -> mediaValidationService.validate(mediaEntity));
    }

    @Test
    void should_not_throw_when_pdf_is_valid_with_no_action() throws IOException {
        PDDocument document = mock(PDDocument.class);
        when(document.getNumberOfPages()).thenReturn(1);
        PDDocumentCatalog catalog = mock(PDDocumentCatalog.class);
        when(document.getDocumentCatalog()).thenReturn(catalog);

        final byte[] data = "data".getBytes();
        loader.when(() -> Loader.loadPDF(data)).thenReturn(document);

        final MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setType("application");
        mediaEntity.setSubType("pdf");
        mediaEntity.setData(data);

        assertDoesNotThrow(() -> mediaValidationService.validate(mediaEntity));
    }

    @Test
    void should_throw_when_media_type_is_forbidden() {
        final String mediaType = "application/x-msdownload";
        final MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setType(mediaType.split("/")[0]);
        mediaEntity.setSubType(mediaType.split("/")[1]);
        mediaEntity.setData(new byte[0]);

        assertThrows(UploadUnauthorized.class, () -> mediaValidationService.validate(mediaEntity));
    }

    @Test
    void should_throw_when_type_mismatch() {
        final String mediaType = "image/jpeg";
        final MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setType(mediaType.split("/")[0]);
        mediaEntity.setSubType(mediaType.split("/")[1]);
        mediaEntity.setData(new byte[0]);

        assertThrows(UploadUnauthorized.class, () -> mediaValidationService.validate(mediaEntity));
    }

    @Test
    void should_throw_when_json_declared_as_image() {
        final String mediaType = "image/json";
        final MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setType(mediaType.split("/")[0]);
        mediaEntity.setSubType(mediaType.split("/")[1]);
        mediaEntity.setData(new byte[0]);

        assertThrows(UploadUnauthorized.class, () -> mediaValidationService.validate(mediaEntity));
    }

    @Test
    void should_not_throw_when_json_declared_as_json() {
        Tika mockedTika = tikaConstruction.constructed().getFirst();
        when(mockedTika.detect(Mockito.any(byte[].class), Mockito.nullable(String.class))).thenReturn("application/json");

        final byte[] data = "data".getBytes();
        final String mediaType = "application/json";
        final MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setType(mediaType.split("/")[0]);
        mediaEntity.setSubType(mediaType.split("/")[1]);
        mediaEntity.setData(data);

        assertDoesNotThrow(() -> mediaValidationService.validate(mediaEntity));
    }
}
