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
    public void shouldSaveImageForPortal() throws Exception {

        File file = new File(MediaRepositoryTest.class.getResource(getTestCasesPath() + "gravitee_logo_anim.gif").toURI());
        InputStream fileInputStream = new FileInputStream(file);

        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] fileBites = IOUtils.toByteArray(fileInputStream);
        long size = fileBites.length;
        byte[] hash = digest.digest(fileBites);
        String hashString = DatatypeConverter.printHexBinary(hash);
        String id = "223344";

        Media imageData = new Media();
        imageData.setId(id);
        imageData.setType("image");
        imageData.setSubType("gif");
        imageData.setFileName("gravitee_logo_anim.gif");
        imageData.setData(fileBites);
        imageData.setSize(file.length());
        imageData.setHash("4692FBACBEF919061ECF328CA543E028");

        String imageId = mediaRepository.save(imageData);
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
    public void shouldSaveImageForAPI() throws Exception {

        File file = new File(MediaRepositoryTest.class.getResource(getTestCasesPath() + "stars.png").toURI());
        InputStream fileInputStream = new FileInputStream(file);


        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] fileBites = IOUtils.toByteArray(fileInputStream);
        long size = fileBites.length;
        byte[] hash = digest.digest(fileBites);
        String hashString = DatatypeConverter.printHexBinary(hash);

        //InputStream targetStream = new ByteArrayInputStream(fileBites);

        Media imageData = new Media();
        imageData.setId("22334455");
        imageData.setType("image");
        imageData.setSubType("png");
        imageData.setFileName("stars.png");
        imageData.setData(fileBites);
        imageData.setSize(size);
        imageData.setApi("123456");
        imageData.setHash(hashString);

        String imageId = mediaRepository.save(imageData);
        Optional<Media> optionalAfter = mediaRepository.findByHash(hashString, "123456", "image");

        assertTrue("Image saved not found", optionalAfter.isPresent());

        final Media imageDataSaved = optionalAfter.get();
        assertEquals("Invalid saved image id.", imageData.getId(), imageDataSaved.getId());
        assertEquals("Invalid saved image name.", imageData.getFileName(), imageDataSaved.getFileName());
        assertEquals("Invalid saved image size.", imageData.getSize(), imageDataSaved.getSize());
    }

//    @Test
//    public void shouldReturnTotalSizeImagesForAPI() throws Exception {
//
//        File file1 = new File(MediaRepositoryTest.class.getResource(getTestCasesPath() + "gravitee_logo_anim.gif").toURI());
//        InputStream fileInputStream = new FileInputStream(file1);
//
//
//        MessageDigest digest = MessageDigest.getInstance("MD5");
//        byte[] fileBites = IOUtils.toByteArray(fileInputStream);
//        long size1 = fileBites.length;
//        byte[] hash = digest.digest(fileBites);
//        String h = DatatypeConverter.printHexBinary(hash);
//
//        Media imageData = new Media();
//        imageData.setId("2233445566");
//        imageData.setType("image");
//        imageData.setSubType("gif");
//        imageData.setFileName("gravitee_logo_anim.gif");
//        imageData.setData(fileBites);
//        imageData.setSize(size1);
//        imageData.setApi("1234567");
//        imageData.setHash(h);
//
//        mediaRepository.save(imageData);
//
//        File file2 = new File(MediaRepositoryTest.class.getResource(getTestCasesPath() + "default_photo.png").toURI());
//        InputStream fileInputStream2 = new FileInputStream(file2);
//
//        fileBites = IOUtils.toByteArray(fileInputStream2);
//        long size2 = fileBites.length;
//        hash = digest.digest(fileBites);
//        h = DatatypeConverter.printHexBinary(hash);
//
//        Media imageData2 = new Media();
//        imageData2.setId("55667788");
//        imageData2.setType("image");
//        imageData2.setSubType("png");
//        imageData2.setFileName("default_photo.png");
//        imageData2.setData(fileBites);
//        imageData2.setSize(size2);
//        imageData2.setApi("1234567");
//        imageData2.setHash(h);
//
//        mediaRepository.save(imageData2);
//
//        long totalSize = mediaRepository.totalSizeFor("1234567", "image");
//
//        //assertEquals("Invalid total image size.", 88458L, totalSize);
//        assertEquals("Invalid total image size.", size1 + size2, totalSize);
//    }

//    @Test
//    public void shouldDeleteImageForPortal() throws Exception {
//        File file = new File(MediaRepositoryTest.class.getResource(getTestCasesPath() + "default_photo.png").toURI());
//        InputStream fileInputStream = new FileInputStream(file);
//
//        MessageDigest digest = MessageDigest.getInstance("MD5");
//        byte[] fileBites = IOUtils.toByteArray(fileInputStream);
//        long size = fileBites.length;
//        byte[] hash = digest.digest(fileBites);
//        String hashString = DatatypeConverter.printHexBinary(hash);
//        String id = "556677";
//
//        Media imageData = new Media();
//        imageData.setId(id);
//        imageData.setType("image");
//        imageData.setSubType("png");
//        imageData.setFileName("default_photo.png");
//        imageData.setData(fileBites);
//        imageData.setSize(size);
//        imageData.setHash(hashString + "2");
//
//        String imageId = mediaRepository.save(imageData);
//        Optional<Media> optionalAfter = mediaRepository.findByHash(hashString + "2", "image");
//
//        assertTrue("Image saved not found", optionalAfter.isPresent());
//
//        final Media imageDataSaved = optionalAfter.get();
//        assertEquals("Invalid saved image id.", imageData.getId(), imageDataSaved.getId());
//        assertEquals("Invalid saved image name.", imageData.getFileName(), imageDataSaved.getFileName());
//
//        mediaRepository.delete(hashString + "2", "image");
//
//        optionalAfter = mediaRepository.findByHash(hashString + "2", "image");
//        assertFalse("Image saved found", optionalAfter.isPresent());
//    }
}
