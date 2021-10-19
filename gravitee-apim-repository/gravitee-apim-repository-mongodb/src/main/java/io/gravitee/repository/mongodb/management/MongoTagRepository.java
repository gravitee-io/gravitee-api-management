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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.TagRepository;
import io.gravitee.repository.management.model.Tag;
import io.gravitee.repository.management.model.TagReferenceType;
import io.gravitee.repository.mongodb.management.internal.api.TagMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.TagMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoTagRepository implements TagRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoTagRepository.class);

    @Autowired
    private TagMongoRepository internalTagRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Tag> findById(String tagId) throws TechnicalException {
        LOGGER.debug("Find tag by ID [{}]", tagId);

        final TagMongo tag = internalTagRepo.findById(tagId).orElse(null);

        LOGGER.debug("Find tag by ID [{}] - Done", tagId);
        return Optional.ofNullable(mapper.map(tag, Tag.class));
    }

    @Override
    public Optional<Tag> findByIdAndReference(String tagId, String referenceId, TagReferenceType referenceType) {
        LOGGER.debug("Find tag by ID and reference [{}, {}, {}]", tagId, referenceId, referenceType);

        final TagMongo tag = internalTagRepo.findByIdAndReferenceIdAndReferenceType(tagId, referenceId, referenceType).orElse(null);

        LOGGER.debug("Find tag by ID and reference[{}, {}, {}] - Done", tagId, referenceId, referenceType);
        return Optional.ofNullable(mapper.map(tag, Tag.class));
    }

    @Override
    public Tag create(Tag tag) throws TechnicalException {
        LOGGER.debug("Create tag [{}]", tag.getName());

        TagMongo tagMongo = mapper.map(tag, TagMongo.class);
        TagMongo createdTagMongo = internalTagRepo.insert(tagMongo);

        Tag res = mapper.map(createdTagMongo, Tag.class);

        LOGGER.debug("Create tag [{}] - Done", tag.getName());

        return res;
    }

    @Override
    public Tag update(Tag tag) throws TechnicalException {
        if (tag == null || tag.getName() == null) {
            throw new IllegalStateException("Tag to update must have a name");
        }

        final TagMongo tagMongo = internalTagRepo.findById(tag.getId()).orElse(null);

        if (tagMongo == null) {
            throw new IllegalStateException(String.format("No tag found with name [%s]", tag.getId()));
        }

        try {
            //Update
            tagMongo.setName(tag.getName());
            tagMongo.setDescription(tag.getDescription());
            tagMongo.setRestrictedGroups(tag.getRestrictedGroups());
            tagMongo.setReferenceId(tag.getReferenceId());
            tagMongo.setReferenceType(tag.getReferenceType());

            TagMongo tagMongoUpdated = internalTagRepo.save(tagMongo);
            return mapper.map(tagMongoUpdated, Tag.class);
        } catch (Exception e) {
            LOGGER.error("An error occured when updating tag", e);
            throw new TechnicalException("An error occured when updating tag");
        }
    }

    @Override
    public void delete(String tagId) throws TechnicalException {
        try {
            internalTagRepo.deleteById(tagId);
        } catch (Exception e) {
            LOGGER.error("An error occured when deleting tag [{}]", tagId, e);
            throw new TechnicalException("An error occured when deleting tag");
        }
    }

    @Override
    public Set<Tag> findByReference(String referenceId, TagReferenceType referenceType) throws TechnicalException {
        final List<TagMongo> tags = internalTagRepo.findByReferenceIdAndReferenceType(referenceId, referenceType);
        return tags.stream().map(tagMongo -> mapper.map(tagMongo, Tag.class)).collect(Collectors.toSet());
    }

    @Override
    public Set<Tag> findAll() throws TechnicalException {
        return internalTagRepo.findAll().stream().map(tagMongo -> mapper.map(tagMongo, Tag.class)).collect(Collectors.toSet());
    }
}
