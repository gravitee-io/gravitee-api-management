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

import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.MediaCriteria;
import io.gravitee.repository.media.model.Media;
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
import org.junit.jupiter.api.Test;

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
        assertTrue(optionalAfter.isPresent(), "Image saved not found");

        final Media imageDataSaved = optionalAfter.get();
        assertNotNull(imageDataSaved.getCreatedAt());
        assertEquals("223344", imageDataSaved.getId(), "Invalid saved image id.");
        assertEquals(fileName, imageDataSaved.getFileName(), "Invalid saved image name.");
        assertEquals(hashString, imageDataSaved.getHash(), "Invalid saved image hash.");
        assertEquals(size, imageDataSaved.getSize().longValue(), "Invalid saved image size.");
        assertEquals("image", imageDataSaved.getType(), "Invalid saved image type.");
        assertEquals("gif", imageDataSaved.getSubType(), "Invalid saved image SubType.");
        assertNotNull(imageDataSaved.getData(), "Invalid saved image data.");

        // test search ignoring type
        optionalAfter = mediaRepository.findByHash(hashString);
        assertTrue(optionalAfter.isPresent(), "Image saved not found");
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

        assertTrue(optionalAfter.isPresent(), "Image saved not found");

        final Media imageDataSaved = optionalAfter.get();
        assertEquals("22334455", imageDataSaved.getId(), "Invalid saved image id.");
        assertEquals(fileName, imageDataSaved.getFileName(), "Invalid saved image name.");
        assertEquals(size, imageDataSaved.getSize().longValue(), "Invalid saved image size.");
        assertNotNull(imageDataSaved.getData(), "Invalid saved image data.");

        // test search ignoring type
        optionalAfter = mediaRepository.findByHash(
            hashString,
            MediaCriteria.builder().api("apiId").organization(ORG_ID).environment(ENV_ID).build()
        );
        assertTrue(optionalAfter.isPresent(), "Image saved not found");
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

        assertNotNull(all, "Assets list not found");
        assertEquals(2, all.size(), "Invalid assets list");

        final Media imageDataSaved = all.get(1);
        assertEquals("image-1", imageDataSaved.getId(), "Invalid saved image id.");
        assertNotNull(imageDataSaved.getSize(), "Invalid saved image size.");
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
        assertFalse(media.isEmpty(), "Should find by hash and API");
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
        assertFalse(media.isEmpty(), "Should find by hash and API");
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
        assertFalse(media.isEmpty(), "Should find by hash and API");
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
        assertNotNull(all, "Assets list not found");
        assertEquals(2, all.size(), "Invalid assets list");

        mediaRepository.deleteAllByApi(apiId);

        Optional<Media> image = mediaRepository.findByHash(
            hashString,
            MediaCriteria.builder().mediaType("image").organization(ORG_ID).environment(ENV_ID).build()
        );
        assertFalse(image.isPresent(), "Invalid asset found");
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

        assertFalse(media.isPresent(), "Should not find media after deletion");
    }

    @Test
    public void should_return_empty_list_with_null_api() throws TechnicalException {
        List<Media> apis = mediaRepository.findAllByApi(null);
        assertTrue(apis.isEmpty(), "Should return empty list with null API");
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

    @Test
    public void should_delete_by_hash_and_environment() throws Exception {
        String hash = "sampleHash123";
        String environment = ENV_ID;
        String fileName = "stars.png";
        byte[] fileBytes = getFileBytes(fileName);
        long size = fileBytes.length;

        createMedia(ORG_ID, environment, fileName, fileBytes, size, hash, "media1", null);

        Optional<Media> mediaBeforeDelete = mediaRepository.findByHash(
            hash,
            MediaCriteria.builder().organization(ORG_ID).environment(environment).build()
        );
        assertTrue(mediaBeforeDelete.isPresent(), "Media should exist before deletion");

        mediaRepository.deleteByHashAndEnvironment(hash, environment);

        Optional<Media> mediaAfterDelete = mediaRepository.findByHash(
            hash,
            MediaCriteria.builder().organization(ORG_ID).environment(environment).build()
        );
        assertFalse(mediaAfterDelete.isPresent(), "Media should not exist after deletion");
    }

    @Test
    public void should_not_delete_with_null_hash_or_environment() throws Exception {
        String fileName = "stars.png";
        byte[] fileBytes = getFileBytes(fileName);
        long size = fileBytes.length;

        createMedia(ORG_ID, ENV_ID, fileName, fileBytes, size, "validHash", "media1", null);

        mediaRepository.deleteByHashAndEnvironment(null, ENV_ID);

        Optional<Media> mediaAfterDeleteAttempt1 = mediaRepository.findByHash(
            "validHash",
            MediaCriteria.builder().organization(ORG_ID).environment(ENV_ID).build()
        );
        assertTrue(mediaAfterDeleteAttempt1.isPresent(), "Media should still exist when hash is null");

        mediaRepository.deleteByHashAndEnvironment("validHash", null);

        Optional<Media> mediaAfterDeleteAttempt2 = mediaRepository.findByHash(
            "validHash",
            MediaCriteria.builder().organization(ORG_ID).environment(ENV_ID).build()
        );
        assertTrue(mediaAfterDeleteAttempt2.isPresent(), "Media should still exist when environment is null");
    }

    @Test
    public void should_delete_one_of_multiple_media_with_same_hash() throws Exception {
        String hash = "duplicateHash";
        String environment1 = "env1";
        String environment2 = "env2";
        String fileName = "stars.png";
        byte[] fileBytes = getFileBytes(fileName);
        long size = fileBytes.length;

        createMedia(ORG_ID, environment1, fileName, fileBytes, size, hash, "media1", null);
        createMedia(ORG_ID, environment2, fileName, fileBytes, size, hash, "media2", null);

        mediaRepository.deleteByHashAndEnvironment(hash, environment1);

        Optional<Media> mediaAfterDeleteEnv1 = mediaRepository.findByHash(
            hash,
            MediaCriteria.builder().organization(ORG_ID).environment(environment1).build()
        );
        assertFalse(mediaAfterDeleteEnv1.isPresent(), "Media with environment1 should be deleted");

        Optional<Media> mediaAfterDeleteEnv2 = mediaRepository.findByHash(
            hash,
            MediaCriteria.builder().organization(ORG_ID).environment(environment2).build()
        );
        assertTrue(mediaAfterDeleteEnv2.isPresent(), "Media with environment2 should still exist");
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
