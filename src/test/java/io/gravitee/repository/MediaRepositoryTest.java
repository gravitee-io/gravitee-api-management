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
package io.gravitee.repository;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.media.model.Media;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Guillaume GILLON
 */
public class MediaRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/media-tests/";
    }

    @Test
    public void shouldCreate() throws Exception {

        File file = new File(MediaRepositoryTest.class.getResource(getTestCasesPath() + "gravitee_logo_anim.gif").toURI());
        InputStream fileInputStream = new FileInputStream(file);

        byte[] fileBites = IOUtils.toByteArray(fileInputStream);
        String id = "223344";

        Media imageData = new Media();
        imageData.setId(id);
        imageData.setType("image");
        imageData.setSubType("gif");
        imageData.setFileName("gravitee_logo_anim.gif");
        imageData.setData(fileBites);
        imageData.setSize(file.length());
        imageData.setHash("4692FBACBEF919061ECF328CA543E028");

        Media mediaCreated = mediaRepository.create(imageData);
        assertNotNull(mediaCreated);

        Optional<Media> optionalAfter = mediaRepository.findByHash("4692FBACBEF919061ECF328CA543E028", "image");
        assertTrue("Image saved not found", optionalAfter.isPresent());

        final Media imageDataSaved = optionalAfter.get();
        assertNotNull(imageDataSaved.getCreatedAt());
        assertEquals("Invalid saved image id.", imageData.getId(), imageDataSaved.getId());
        assertEquals("Invalid saved image name.", imageData.getFileName(), imageDataSaved.getFileName());
        assertEquals("Invalid saved image hash.", imageData.getHash(), imageDataSaved.getHash());
        assertEquals("Invalid saved image size.", imageData.getSize(), imageDataSaved.getSize());
        assertEquals("Invalid saved image type.", imageData.getType(), imageDataSaved.getType());
        assertEquals("Invalid saved image SubType.", imageData.getSubType(), imageDataSaved.getSubType());
    }

    @Test
    public void shouldCreateForAPI() throws Exception {

        File file = new File(MediaRepositoryTest.class.getResource(getTestCasesPath() + "stars.png").toURI());
        InputStream fileInputStream = new FileInputStream(file);

        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] fileBites = IOUtils.toByteArray(fileInputStream);
        long size = fileBites.length;
        byte[] hash = digest.digest(fileBites);
        String hashString = DatatypeConverter.printHexBinary(hash);

        Media imageData = new Media();
        imageData.setId("22334455");
        imageData.setType("image");
        imageData.setSubType("png");
        imageData.setFileName("stars.png");
        imageData.setData(fileBites);
        imageData.setSize(size);
        imageData.setApi("apiId");
        imageData.setHash(hashString);

        Media mediaCreated = mediaRepository.create(imageData);
        assertNotNull(mediaCreated);

        Optional<Media> optionalAfter = mediaRepository.findByHashAndApi(hashString, "apiId", "image");
        assertTrue("Image saved not found", optionalAfter.isPresent());

        final Media imageDataSaved = optionalAfter.get();
        assertEquals("Invalid saved image id.", imageData.getId(), imageDataSaved.getId());
        assertEquals("Invalid saved image name.", imageData.getFileName(), imageDataSaved.getFileName());
        assertEquals("Invalid saved image size.", imageData.getSize(), imageDataSaved.getSize());
    }

    @Test
    public void shouldFindAllForAnAPI() throws Exception {

        File file = new File(MediaRepositoryTest.class.getResource(getTestCasesPath() + "stars.png").toURI());
        InputStream fileInputStream = new FileInputStream(file);
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] fileBites = IOUtils.toByteArray(fileInputStream);
        long size = fileBites.length;
        byte[] hash = digest.digest(fileBites);
        String hashString = DatatypeConverter.printHexBinary(hash);
        String apiId = "myApi";
        for (int i = 0; i < 2; i++) {
            Media imageData = new Media();
            String id = "image-" + i;
            imageData.setId(id);
            imageData.setType("image");
            imageData.setSubType("png");
            imageData.setFileName("stars.png");
            imageData.setData(fileBites);
            imageData.setSize(size);
            imageData.setApi(apiId);
            imageData.setHash(hashString);
            mediaRepository.create(imageData);
        }

        Media imageData = new Media();
        String id = "fakeImg";
        imageData.setId(id);
        imageData.setType("image");
        imageData.setSubType("png");
        imageData.setFileName("stars.png");
        imageData.setData(fileBites);
        imageData.setSize(size);
        imageData.setApi("fakeApi");
        imageData.setHash(hashString);
        mediaRepository.create(imageData);

        List<Media> all = mediaRepository.findAllByApi(apiId);

        assertNotNull("Assets list not found", all);
        assertEquals("Invalid assets list", 2, all.size());

        final Media imageDataSaved = all.get(1);
        assertEquals("Invalid saved image id.", "image-1", imageDataSaved.getId());
        assertNotNull("Invalid saved image size.", imageDataSaved.getSize());
    }

    @Test
    public void shouldDeleteAllForAnAPI() throws Exception {

        File file = new File(MediaRepositoryTest.class.getResource(getTestCasesPath() + "stars.png").toURI());
        InputStream fileInputStream = new FileInputStream(file);
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] fileBites = IOUtils.toByteArray(fileInputStream);
        long size = fileBites.length;
        byte[] hash = digest.digest(fileBites);
        String hashString = DatatypeConverter.printHexBinary(hash);
        String apiId = "myApi";
        for (int i = 0; i < 2; i++) {
            Media imageData = new Media();
            String id = "image-" + i;
            imageData.setId(id);
            imageData.setType("image");
            imageData.setSubType("png");
            imageData.setFileName("stars.png");
            imageData.setData(fileBites);
            imageData.setSize(size);
            imageData.setApi(apiId);
            imageData.setHash(hashString);
            mediaRepository.create(imageData);
        }

        List<Media> all = mediaRepository.findAllByApi(apiId);
        assertNotNull("Assets list not found", all);
        assertEquals("Invalid assets list", 2, all.size());

        mediaRepository.deleteAllByApi(apiId);

        Optional<Media> image = mediaRepository.findByHash(hashString, "image");
        assertFalse("Invalid asset found", image.isPresent());
    }


}
