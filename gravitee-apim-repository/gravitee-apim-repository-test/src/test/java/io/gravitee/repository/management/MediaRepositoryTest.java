/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.management;

import static org.junit.Assert.*;

import io.gravitee.repository.config.AbstractManagementRepositoryTest;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.media.model.Media;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import javax.xml.bind.DatatypeConverter;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

/**
 * @author Guillaume GILLON
 */
public class MediaRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/media-tests/";
    }

    @Test
    public void shouldCreate() throws Exception {
        String fileName = "gravitee_logo_anim.gif";
        byte[] fileBytes = getFileBytes(fileName);
        long size = fileBytes.length;
        String hashString = getHashString(fileBytes);

        Media mediaCreated = createMedia(fileName, fileBytes, size, hashString, "223344", null);
        assertNotNull(mediaCreated);

        Optional<Media> optionalAfter = mediaRepository.findByHashAndType(hashString, "image");
        assertTrue("Image saved not found", optionalAfter.isPresent());

        final Media imageDataSaved = optionalAfter.get();
        assertNotNull(imageDataSaved.getCreatedAt());
        assertEquals("Invalid saved image id.", "223344", imageDataSaved.getId());
        assertEquals("Invalid saved image name.", fileName, imageDataSaved.getFileName());
        assertEquals("Invalid saved image hash.", hashString, imageDataSaved.getHash());
        assertEquals("Invalid saved image size.", size, imageDataSaved.getSize().longValue());
        assertEquals("Invalid saved image type.", "image", imageDataSaved.getType());
        assertEquals("Invalid saved image SubType.", "gif", imageDataSaved.getSubType());
        assertNotNull("Invalid saved image data.", imageDataSaved.getData());

        // test search ignoring type
        optionalAfter = mediaRepository.findByHash(hashString);
        assertTrue("Image saved not found", optionalAfter.isPresent());
    }

    @Test
    public void shouldCreateForAPI() throws Exception {
        String fileName = "stars.png";
        byte[] fileBytes = getFileBytes(fileName);
        long size = fileBytes.length;
        String hashString = getHashString(fileBytes);

        createMedia(fileName, fileBytes, size, hashString, "22334455", "apiId");

        Optional<Media> optionalAfter = mediaRepository.findByHashAndApiAndType(hashString, "apiId", "image");

        assertTrue("Image saved not found", optionalAfter.isPresent());

        final Media imageDataSaved = optionalAfter.get();
        assertEquals("Invalid saved image id.", "22334455", imageDataSaved.getId());
        assertEquals("Invalid saved image name.", fileName, imageDataSaved.getFileName());
        assertEquals("Invalid saved image size.", size, imageDataSaved.getSize().longValue());
        assertNotNull("Invalid saved image data.", imageDataSaved.getData());

        // test search ignoring type
        optionalAfter = mediaRepository.findByHashAndApi(hashString, "apiId");
        assertTrue("Image saved not found", optionalAfter.isPresent());
    }

    @Test
    public void shouldFindMediaWithoutContent() throws Exception {
        String fileName = "stars.png";
        byte[] fileBytes = getFileBytes(fileName);
        long size = fileBytes.length;
        String hashString = getHashString(fileBytes);

        createMedia(fileName, fileBytes, size, hashString, "2233445566", null);

        Optional<Media> optionalAfter = mediaRepository.findByHash(hashString, false);
        assertTrue(optionalAfter.isPresent());
        assertNull(optionalAfter.get().getData());
    }

    @Test
    public void shouldFindAllForAnAPI() throws Exception {
        String fileName = "stars.png";
        byte[] fileBytes = getFileBytes(fileName);
        long size = fileBytes.length;
        String hashString = getHashString(fileBytes);
        String apiId = "myApi";

        for (int i = 0; i < 2; i++) {
            String id = "image-" + i;
            createMedia(fileName, fileBytes, size, hashString, id, apiId);
        }

        createMedia(fileName, fileBytes, size, hashString, "fakeImg", "fakeApi");

        List<Media> all = mediaRepository.findAllByApi(apiId);

        assertNotNull("Assets list not found", all);
        assertEquals("Invalid assets list", 2, all.size());

        final Media imageDataSaved = all.get(1);
        assertEquals("Invalid saved image id.", "image-1", imageDataSaved.getId());
        assertNotNull("Invalid saved image size.", imageDataSaved.getSize());
    }

    @Test
    public void shouldDeleteAllForAnAPI() throws Exception {
        String fileName = "stars.png";
        byte[] fileBytes = getFileBytes(fileName);
        long size = fileBytes.length;
        String hashString = getHashString(fileBytes);
        String apiId = "myApi";

        for (int i = 0; i < 2; i++) {
            String id = "image-" + i;
            createMedia(fileName, fileBytes, size, hashString, id, apiId);
        }

        List<Media> all = mediaRepository.findAllByApi(apiId);
        assertNotNull("Assets list not found", all);
        assertEquals("Invalid assets list", 2, all.size());

        mediaRepository.deleteAllByApi(apiId);

        Optional<Media> image = mediaRepository.findByHashAndType(hashString, "image");
        assertFalse("Invalid asset found", image.isPresent());
    }

    private Media createMedia(String fileName, byte[] fileBytes, long size, String hashString, String id, String api)
        throws TechnicalException {
        Media imageData = new Media();
        imageData.setId(id);
        imageData.setType("image");
        imageData.setSubType(fileName.substring(fileName.lastIndexOf('.') + 1));
        imageData.setFileName(fileName);
        imageData.setData(fileBytes);
        imageData.setSize(size);
        imageData.setHash(hashString);
        if (api != null) {
            imageData.setApi(api);
        }

        return mediaRepository.create(imageData);
    }

    private String getHashString(byte[] fileBytes) throws NoSuchAlgorithmException {
        byte[] hash = MessageDigest.getInstance("MD5").digest(fileBytes);
        return DatatypeConverter.printHexBinary(hash);
    }

    private byte[] getFileBytes(String fileName) throws URISyntaxException, IOException {
        File file = new File(MediaRepositoryTest.class.getResource(getTestCasesPath() + fileName).toURI());
        InputStream fileInputStream = new FileInputStream(file);
        return IOUtils.toByteArray(fileInputStream);
    }
}
