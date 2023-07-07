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
package io.gravitee.repository.mongodb.management;

import static com.mongodb.client.model.Filters.*;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.media.api.MediaRepository;
import io.gravitee.repository.media.model.Media;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume GILLON
 * @author GraviteeSource Team
 */
@Component
public class MongoMediaRepository implements MediaRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoMediaRepository.class);

    @Autowired
    private MongoDatabaseFactory mongoFactory;

    @Override
    public Media create(Media media) {
        Document doc = new Document()
            .append("type", media.getType())
            .append("subType", media.getSubType())
            .append("size", media.getSize())
            .append("hash", media.getHash());

        if (media.getApi() != null) {
            doc.append("api", media.getApi());
        }

        GridFSUploadOptions options = new GridFSUploadOptions().metadata(doc);

        getGridFs()
            .uploadFromStream(new BsonString(media.getId()), media.getFileName(), new ByteArrayInputStream(media.getData()), options);

        return media;
    }

    @Override
    public Optional<Media> findByHash(String hash) throws TechnicalException {
        return this.findByHashAndApiAndType(hash, null, null, true);
    }

    @Override
    public Optional<Media> findByHash(String hash, boolean withContent) throws TechnicalException {
        return this.findByHashAndApiAndType(hash, null, null, withContent);
    }

    @Override
    public Optional<Media> findByHashAndApi(String hash, String api) throws TechnicalException {
        return this.findByHashAndApiAndType(hash, api, null, true);
    }

    @Override
    public Optional<Media> findByHashAndApi(String hash, String api, boolean withContent) throws TechnicalException {
        return this.findByHashAndApiAndType(hash, api, null, withContent);
    }

    @Override
    public Optional<Media> findByHashAndType(String hash, String mediaType) throws TechnicalException {
        return this.findByHashAndApiAndType(hash, null, mediaType, true);
    }

    @Override
    public Optional<Media> findByHashAndApiAndType(String hash, String api, String mediaType) throws TechnicalException {
        return this.findByHashAndApiAndType(hash, api, mediaType, true);
    }

    @Override
    public Optional<Media> findByHashAndApiAndType(String hash, String api, String mediaType, boolean withContent)
        throws TechnicalException {
        return this.findFirst(this.getQueryFindMedia(hash, api, mediaType), withContent);
    }

    @Override
    public List<Media> findAllByApi(String api) {
        if (api != null) {
            Bson apiQuery = eq("metadata.api", api);
            return this.findAll(apiQuery);
        }
        LOGGER.warn("Returning an empty list because the API identifier given as an argument is null");
        return new ArrayList<>();
    }

    private List<Media> findAll(Bson query) {
        GridFSFindIterable files = getGridFs().find(query);
        ArrayList<Media> all = new ArrayList<>();
        files.forEach(
            (Consumer<GridFSFile>) file -> {
                Media convert = convert(file, true);
                if (convert != null) {
                    all.add(convert);
                }
            }
        );
        return all;
    }

    private Optional<Media> findFirst(Bson query, boolean withContent) throws TechnicalException {
        try {
            GridFSFile file = getGridFs().find(query).first();
            Media imageData = convert(file, withContent);
            return Optional.ofNullable(imageData);
        } catch (Exception e) {
            throw new TechnicalException(e);
        }
    }

    private Media convert(GridFSFile file, boolean withContent) {
        Media imageData = null;
        if (file != null) {
            InputStream inputStream = getGridFs().openDownloadStream(file.getId());
            Document metadata = file.getMetadata();

            imageData = new Media();
            imageData.setId(file.getId().asString().getValue());
            imageData.setCreatedAt(file.getUploadDate());
            imageData.setType((String) metadata.get("type"));
            imageData.setSubType((String) metadata.get("subType"));
            imageData.setSize((Long) metadata.get("size"));
            imageData.setFileName(file.getFilename());
            imageData.setHash((String) metadata.get("hash"));

            if (withContent) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] result = null;
                try {
                    int next = inputStream.read();

                    while (next > -1) {
                        bos.write(next);
                        next = inputStream.read();
                    }
                    bos.flush();
                    result = bos.toByteArray();
                    bos.close();
                } catch (IOException e) {
                    LOGGER.error("An error as occurred while converting GridFs file to media", e);
                }
                imageData.setData(result);
            }
        }
        return imageData;
    }

    private Bson getQueryFindMedia(String hash, String api, String mediaType) {
        Bson addQuery = api == null ? not(exists("metadata.api")) : eq("metadata.api", api);
        if (mediaType == null) {
            return and(eq("metadata.hash", hash), addQuery);
        }
        return and(eq("metadata.type", mediaType), eq("metadata.hash", hash), addQuery);
    }

    private GridFSBucket getGridFs() {
        MongoDatabase db = mongoFactory.getMongoDatabase();
        String bucketName = "media";
        return GridFSBuckets.create(db, bucketName);
    }

    @Override
    public void deleteAllByApi(String api) throws TechnicalException {
        if (api == null) {
            LOGGER.warn("Skipping media deletion because the API identifier given as an argument is null");
        } else {
            doDeleteAllByApi(api);
        }
    }

    private void doDeleteAllByApi(String api) throws TechnicalException {
        try {
            Bson apiQuery = eq("metadata.api", api);
            GridFSBucket gridFs = getGridFs();
            GridFSFindIterable files = gridFs.find(apiQuery);
            files.forEach((Consumer<GridFSFile>) gridFSFile -> gridFs.delete(gridFSFile.getId()));
        } catch (Exception e) {
            throw new TechnicalException(e);
        }
    }

    @Override
    public void deleteByHashAndApi(String hash, String api) throws TechnicalException {
        try {
            Bson query = and(eq("metadata.api", api), eq("metadata.hash", hash));
            GridFSBucket gridFs = getGridFs();
            gridFs.find(query).forEach(gridFSFile -> gridFs.delete(gridFSFile.getId()));
        } catch (Exception e) {
            throw new TechnicalException(e);
        }
    }
}
