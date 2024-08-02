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
package io.gravitee.repository.management;

import static org.junit.Assert.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.MediaCriteria;
import io.gravitee.repository.media.model.Media;
import io.vertx.core.spi.launcher.ExecutionContext;
import jakarta.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

/**
 * @author Guillaume GILLON
 */
public class MediaRepositoryTest extends AbstractManagementRepositoryTest {

    private static final String ORG_ID = "org#1";
    private static final String ENV_ID = "env#1";

    @Override
    protected String getTestCasesPath() {
        return "/data/media-tests/";
    }

    @Test
    public void should_create() throws Exception {
        String fileName = "gravitee_logo_anim.gif";
        byte[] fileBytes = getFileBytes(fileName);
        long size = fileBytes.length;
        String hashString = getHashString(fileBytes);

        Media mediaCreated = createMedia(ORG_ID, ENV_ID, fileName, fileBytes, size, hashString, "223344", null);
        assertNotNull(mediaCreated);

        Optional<Media> optionalAfter = mediaRepository.findByHash(
            hashString,
            MediaCriteria.builder().mediaType("image").organization(ORG_ID).environment(ENV_ID).build()
        );
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
    public void should_create_for_api() throws Exception {
        String fileName = "stars.png";
        byte[] fileBytes = getFileBytes(fileName);
        long size = fileBytes.length;
        String hashString = getHashString(fileBytes);

        createMedia(ORG_ID, ENV_ID, fileName, fileBytes, size, hashString, "22334455", "apiId");

        Optional<Media> optionalAfter = mediaRepository.findByHash(
            hashString,
            MediaCriteria.builder().api("apiId").organization(ORG_ID).environment(ENV_ID).mediaType("image").build()
        );

        assertTrue("Image saved not found", optionalAfter.isPresent());

        final Media imageDataSaved = optionalAfter.get();
        assertEquals("Invalid saved image id.", "22334455", imageDataSaved.getId());
        assertEquals("Invalid saved image name.", fileName, imageDataSaved.getFileName());
        assertEquals("Invalid saved image size.", size, imageDataSaved.getSize().longValue());
        assertNotNull("Invalid saved image data.", imageDataSaved.getData());

        // test search ignoring type
        optionalAfter =
            mediaRepository.findByHash(hashString, MediaCriteria.builder().api("apiId").organization(ORG_ID).environment(ENV_ID).build());
        assertTrue("Image saved not found", optionalAfter.isPresent());
    }

    @Test
    public void should_find_by_hash_and_api_without_content() throws Exception {
        String apiId = "c2f71615-6db0-48fb-b4b0-ccd6cf33dce8";
        String mediaId = "294b50da-01eb-4dce-ace8-94f98bb07787";
        String fileName = "stars.png";

        byte[] fileBytes = getFileBytes(fileName);
        long size = fileBytes.length;

        String hashString = getHashString(fileBytes);

        createMedia(ORG_ID, ENV_ID, fileName, fileBytes, size, hashString, mediaId, apiId);

        Optional<Media> optionalAfter = mediaRepository.findByHash(
            hashString,
            MediaCriteria.builder().api(apiId).organization(ORG_ID).environment(ENV_ID).build(),
            false
        );
        assertTrue(optionalAfter.isPresent());
        assertNull(optionalAfter.get().getData());
    }

    @Test
    public void should_find_all_for_an_api() throws Exception {
        String fileName = "stars.png";
        byte[] fileBytes = getFileBytes(fileName);
        long size = fileBytes.length;
        String hashString = getHashString(fileBytes);
        String apiId = "myApi";

        for (int i = 0; i < 2; i++) {
            String id = "image-" + i;
            createMedia(ORG_ID, ENV_ID, fileName, fileBytes, size, hashString, id, apiId);
        }

        createMedia(ORG_ID, ENV_ID, fileName, fileBytes, size, hashString, "fakeImg", "fakeApi");

        List<Media> all = mediaRepository.findAllByApi(apiId);

        assertNotNull("Assets list not found", all);
        assertEquals("Invalid assets list", 2, all.size());

        final Media imageDataSaved = all.get(1);
        assertEquals("Invalid saved image id.", "image-1", imageDataSaved.getId());
        assertNotNull("Invalid saved image size.", imageDataSaved.getSize());
    }

    @Test
    public void should_find_by_hash_and_api() throws Exception {
        String apiId = "221933a0-1d15-4638-adf1-6e5d0523c894";
        String mediaId = "833419c4-4fde-430b-9a60-58c12f5fe4fb";
        String fileName = "stars.png";

        byte[] fileBytes = getFileBytes(fileName);
        long size = fileBytes.length;

        String hashString = getHashString(fileBytes);

        createMedia(ORG_ID, ENV_ID, fileName, fileBytes, size, hashString, mediaId, apiId);

        Optional<Media> media = mediaRepository.findByHash(
            hashString,
            MediaCriteria.builder().api(apiId).organization(ORG_ID).environment(ENV_ID).build()
        );
        assertFalse("Should find by hash and API", media.isEmpty());
    }

    @Test
    public void should_find_by_hash_and_api_and_type() throws Exception {
        String apiId = "7ecb2e06-24d7-4077-acf7-b806dcb57f35";
        String mediaId = "f5a2745f-754c-46e9-abe8-077a621816f1";
        String fileName = "stars.png";

        byte[] fileBytes = getFileBytes(fileName);
        long size = fileBytes.length;

        String hashString = getHashString(fileBytes);

        createMedia(ORG_ID, ENV_ID, fileName, fileBytes, size, hashString, mediaId, apiId);

        Optional<Media> media = mediaRepository.findByHash(
            hashString,
            MediaCriteria.builder().api(apiId).mediaType("image").organization(ORG_ID).environment(ENV_ID).build()
        );
        assertFalse("Should find by hash and API", media.isEmpty());
    }

    @Test
    public void should_find_without_environment_and_organization() throws Exception {
        String apiId = "7ecb2e06-24d7-4077-acf7-b806dcb5123";
        String mediaId = "f5a2745f-754c-46e9-abe8-077a62181123";
        String fileName = "stars.png";

        byte[] fileBytes = getFileBytes(fileName);
        long size = fileBytes.length;

        String hashString = getHashString(fileBytes);

        createMedia(null, null, fileName, fileBytes, size, hashString, mediaId, apiId);

        Optional<Media> media = mediaRepository.findByHash(
            hashString,
            MediaCriteria.builder().api(apiId).mediaType("image").organization(ORG_ID).environment(ENV_ID).build()
        );
        assertFalse("Should find by hash and API", media.isEmpty());
        assertEquals(mediaId, media.get().getId());
    }

    @Test
    public void should_delete_all_for_an_api() throws Exception {
        String fileName = "stars.png";
        byte[] fileBytes = getFileBytes(fileName);
        long size = fileBytes.length;
        String hashString = getHashString(fileBytes);
        String apiId = "myApi";

        for (int i = 0; i < 2; i++) {
            String id = "image-" + i;
            createMedia(ORG_ID, ENV_ID, fileName, fileBytes, size, hashString, id, apiId);
        }

        List<Media> all = mediaRepository.findAllByApi(apiId);
        assertNotNull("Assets list not found", all);
        assertEquals("Invalid assets list", 2, all.size());

        mediaRepository.deleteAllByApi(apiId);

        Optional<Media> image = mediaRepository.findByHash(
            hashString,
            MediaCriteria.builder().mediaType("image").organization(ORG_ID).environment(ENV_ID).build()
        );
        assertFalse("Invalid asset found", image.isPresent());
    }

    @Test
    public void should_delete_by_hash_and_api_id() throws Exception {
        String apiId = "1ba38b69-446d-4722-9208-024153fc500f";
        String mediaId = "542449ec-7dd7-4e35-bdcc-735b8788b0cc";
        String fileName = "stars.png";

        byte[] fileBytes = getFileBytes(fileName);
        long size = fileBytes.length;

        String hashString = getHashString(fileBytes);

        createMedia(ORG_ID, ENV_ID, fileName, fileBytes, size, hashString, mediaId, apiId);

        mediaRepository.deleteByHashAndApi(hashString, apiId);

        Optional<Media> media = mediaRepository.findByHash(hashString, MediaCriteria.builder().api(apiId).build());

        assertFalse("Should not find media after deletion", media.isPresent());
    }

    @Test
    public void should_return_empty_list_with_null_api() throws TechnicalException {
        List<Media> apis = mediaRepository.findAllByApi(null);
        assertTrue("Should return empty list with null API", apis.isEmpty());
    }

    @Test
    public void should_delete_by_organization() throws Exception {
        String fileName = "stars.png";
        byte[] fileBytes = getFileBytes(fileName);
        long size = fileBytes.length;
        String hashString = getHashString(fileBytes);
        String apiId = "deleted-api";

        for (int i = 0; i < 2; i++) {
            createMedia("organization#" + i, ENV_ID, fileName, fileBytes, size, hashString, "image-" + i, apiId);
        }

        int nbBeforeDeletion = mediaRepository.findAllByApi(apiId).size();
        List<String> deleted = mediaRepository.deleteByOrganization("organization#1");
        int nbAfterDeletion = mediaRepository.findAllByApi(apiId).size();

        assertEquals(2, nbBeforeDeletion);
        assertEquals(1, deleted.size());
        assertEquals(1, nbAfterDeletion);
    }

    @Test
    public void should_delete_by_environment() throws Exception {
        String fileName = "stars.png";
        byte[] fileBytes = getFileBytes(fileName);
        long size = fileBytes.length;
        String hashString = getHashString(fileBytes);
        String apiId = "deleted-api";

        for (int i = 0; i < 2; i++) {
            createMedia(ORG_ID, "environment#" + i, fileName, fileBytes, size, hashString, "image-" + i, apiId);
        }

        int nbBeforeDeletion = mediaRepository.findAllByApi(apiId).size();
        List<String> deleted = mediaRepository.deleteByEnvironment("environment#1");
        int nbAfterDeletion = mediaRepository.findAllByApi(apiId).size();

        assertEquals(2, nbBeforeDeletion);
        assertEquals(1, deleted.size());
        assertEquals(1, nbAfterDeletion);
    }

    private Media createMedia(
        String organization,
        String environment,
        String fileName,
        byte[] fileBytes,
        long size,
        String hashString,
        String id,
        String api
    ) throws TechnicalException {
        Media imageData = new Media();
        imageData.setId(id);
        imageData.setType("image");
        imageData.setSubType(fileName.substring(fileName.lastIndexOf('.') + 1));
        imageData.setFileName(fileName);
        imageData.setData(fileBytes);
        imageData.setSize(size);
        imageData.setHash(hashString);
        if (organization != null) {
            imageData.setOrganization(organization);
        }
        if (environment != null) {
            imageData.setEnvironment(environment);
        }
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
