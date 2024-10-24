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

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.MediaCriteria;
import io.gravitee.repository.media.api.MediaRepository;
import io.gravitee.repository.media.model.Media;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
            .append("hash", media.getHash())
            .append("environment", media.getEnvironment())
            .append("organization", media.getOrganization());

        if (media.getApi() != null) {
            doc.append("api", media.getApi());
        }

        GridFSUploadOptions options = new GridFSUploadOptions().metadata(doc);

        getGridFs()
            .uploadFromStream(new BsonString(media.getId()), media.getFileName(), new ByteArrayInputStream(media.getData()), options);

        return media;
    }

    @Override
    public Optional<Media> findByHash(String hash, MediaCriteria mediaCriteria, boolean withContent) throws TechnicalException {
        return this.findFirst(getQueryFindMedia(hash, mediaCriteria), withContent);
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
        files.forEach(file -> {
            Media convert = convert(file, true);
            if (convert != null) {
                all.add(convert);
            }
        });
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
            Document metadata = file.getMetadata();

            imageData = new Media();
            imageData.setId(file.getId().asString().getValue());
            imageData.setCreatedAt(file.getUploadDate());
            imageData.setType((String) metadata.get("type"));
            imageData.setSubType((String) metadata.get("subType"));
            imageData.setSize((Long) metadata.get("size"));
            imageData.setFileName(file.getFilename());
            imageData.setHash((String) metadata.get("hash"));
            imageData.setApi((String) metadata.get("api"));
            imageData.setEnvironment((String) metadata.get("environment"));
            imageData.setOrganization((String) metadata.get("organization"));

            if (withContent) {
                InputStream inputStream = getGridFs().openDownloadStream(file.getId());
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

    private static Bson getQueryFindMedia(String hash, MediaCriteria mediaCriteria) {
        final List<Bson> filters = new ArrayList<>();
        filters.add(eq("metadata.hash", hash));

        if (mediaCriteria == null || mediaCriteria.getApi() == null) {
            filters.add(not(exists("metadata.api")));
        } else {
            filters.add(eq("metadata.api", mediaCriteria.getApi()));
        }

        Bson contextQuery = getContextQuery(mediaCriteria);

        if (contextQuery != null) {
            filters.add(contextQuery);
        }

        if (mediaCriteria != null && mediaCriteria.getMediaType() != null) {
            filters.add(eq("metadata.type", mediaCriteria.getMediaType()));
        }

        return and(filters);
    }

    private static Bson getContextQuery(MediaCriteria mediaCriteria) {
        if (mediaCriteria == null) {
            return null;
        }

        Bson environmentQuery = getEnvironmentQuery(mediaCriteria.getEnvironment());
        Bson organizationQuery = getOrganizationQuery(mediaCriteria.getOrganization());

        if (environmentQuery != null && organizationQuery != null) {
            return and(environmentQuery, organizationQuery);
        } else if (environmentQuery != null) {
            return environmentQuery;
        } else return organizationQuery;
    }

    private static Bson getEnvironmentQuery(String environment) {
        if (environment == null) {
            return null;
        }
        return or(eq("metadata.environment", environment), not(exists("metadata.environment")), eq("metadata.environment", null));
    }

    private static Bson getOrganizationQuery(String organization) {
        if (organization == null) {
            return null;
        }
        return or(eq("metadata.organization", organization), not(exists("metadata.organization")), eq("metadata.organization", null));
    }

    private GridFSBucket getGridFs() {
        return GridFSBuckets.create(mongoFactory.getMongoDatabase(), "media");
    }

    @Override
    public void deleteAllByApi(String api) throws TechnicalException {
        deleteWithMetadata("metadata.api", api);
    }

    @Override
    public void deleteByHashAndApi(String hash, String api) throws TechnicalException {
        if (hash == null || api == null) {
            LOGGER.warn("Skipping media deletion because the [{}/{}] given as an argument is null", hash, api);
        } else {
            deleteWithQuery(and(eq("metadata.api", api), eq("metadata.hash", hash)));
        }
    }

    public void deleteByHashAndEnvironment(String hash, String environment) throws TechnicalException {
        if (hash == null || environment == null) {
            LOGGER.warn("Skipping media deletion because the [{}/{}] given as an argument is null", hash, environment);
        } else {
            deleteWithQuery(and(eq("metadata.environment", environment), eq("metadata.hash", hash)));
        }
    }

    @Override
    public List<String> deleteByEnvironment(String environment) throws TechnicalException {
        return deleteWithMetadata("metadata.environment", environment);
    }

    @Override
    public List<String> deleteByOrganization(String organization) throws TechnicalException {
        return deleteWithMetadata("metadata.organization", organization);
    }

    private List<String> deleteWithMetadata(String metadata, String value) throws TechnicalException {
        if (metadata == null || value == null) {
            LOGGER.warn("Skipping media deletion because the [{}] given as an argument is null", metadata);
        } else {
            return deleteWithQuery(eq(metadata, value));
        }
        return Collections.emptyList();
    }

    private List<String> deleteWithQuery(Bson query) throws TechnicalException {
        try {
            GridFSBucket gridFs = getGridFs();
            List<String> deleted = new ArrayList<>();
            gridFs
                .find(query)
                .forEach(file -> {
                    deleted.add(file.getId().asString().getValue());
                    gridFs.delete(file.getId());
                });
            return deleted;
        } catch (Exception e) {
            throw new TechnicalException(e);
        }
    }
}
