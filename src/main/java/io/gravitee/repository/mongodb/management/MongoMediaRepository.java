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
package io.gravitee.repository.mongodb.management;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import io.gravitee.repository.media.api.MediaRepository;
import io.gravitee.repository.media.model.Media;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.not;

/**
 * @author Guillaume GILLON
 */
@Component
public class MongoMediaRepository implements MediaRepository {


    private Map<String, GridFsOperations> gridFsOperationsMap = new HashMap<>(5);

    @Autowired
    private MongoDbFactory mongoFactory;

    @Autowired
    private MappingMongoConverter converter;


    @Override
    public String save(Media media) {

        Document doc = new Document()
                .append("type", media.getType())
                .append("subType", media.getSubType())
                .append("size", media.getSize())
                .append("hash", media.getHash());

        if(media.getApi() != null) {
            doc.append("api", media.getApi());
        }

        GridFSUploadOptions options = new GridFSUploadOptions()
                .metadata(doc);

        getGridFs()
                .uploadFromStream(
                    new BsonString(media.getId()),
                    media.getFileName(),
                    new ByteArrayInputStream(media.getData()),
                    options
                );

        return media.getId();
    }

    @Override
    public Optional<Media> findByHash(String hash, String mediaType) {
        return this.findByHash(hash, null, mediaType);
    }

    @Override
    public Optional<Media> findByHash(String hash, String api, String mediaType) {
        return this.finder(this.getQueryFindMedia(hash, api, mediaType));
    }

    private Optional<Media> finder(Bson query) {
        Bson filter = null;

        GridFSFile file = getGridFs().find(query).first();

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
                e.printStackTrace();
            }
            imageData.setData(result);
            }

        return Optional.ofNullable(imageData);
    }

    @Override
    public void delete(String hash, String mediaType) {
        this.deleteApiFor(hash, null, mediaType);
    }

    @Override
    public void deleteApiFor(String hash, String api, String mediaType) {
        GridFSFile file = getGridFs().find(getQueryFindMedia(hash, api, mediaType)).first();

        getGridFs().delete(file.getId());

        /*Criteria criteria = null;

        if(api == null) {
            criteria = Criteria.where("metadata.hash").is(hash).and("metadata.api").exists(false);
        } else {
            criteria = Criteria.where("metadata.hash").is(hash).and("metadata.api").is(api);
        }
        Query query = Query.query(criteria);
        getGridOperation(mediaType).delete(query);*/
    }

    private Bson getQueryFindMedia(String hash, String api, String mediaType) {
        Bson addQuery = api == null ? not(exists("metadata.api")) : eq("metadata.api", api);
        return and(eq("metadata.type", mediaType), eq("metadata.hash", hash), addQuery);
    }


    @Override
    public long totalSizeFor(String api, String mediaType) {

        GridFSFindIterable gridFSFindIterable = getGridFs().find();

        long sum = 0;
        for (GridFSFile gridFSFile : gridFSFindIterable) {
            sum += (Long)gridFSFile.getMetadata().get("size");
        }

        return sum;
    }

    private GridFSBucket getGridFs() {

        MongoDatabase db = mongoFactory.getDb();
        String bucketName = "media";
        //String bucketName = api == null ? mediaType : mediaType + "-" + api;
        return GridFSBuckets.create(db, bucketName);
    }
}
