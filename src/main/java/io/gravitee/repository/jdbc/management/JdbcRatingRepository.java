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
package io.gravitee.repository.jdbc.management;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.RatingRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Rating;
import io.gravitee.repository.management.model.RatingReferenceType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static java.lang.String.format;

import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;

/**
 *
 * @author njt
 */
@Repository
public class JdbcRatingRepository extends JdbcAbstractCrudRepository<Rating, String> implements RatingRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcRatingRepository.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Rating.class, "ratings", "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, RatingReferenceType.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("user", Types.NVARCHAR, String.class)
            .addColumn("rate", Types.TINYINT, byte.class)
            .addColumn("title", Types.NVARCHAR, String.class)
            .addColumn("comment", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
    }

    @Override
    protected String getId(Rating item) {
        return item.getId();
    }

    @Override
    public Rating create(Rating item) throws TechnicalException {
        LOGGER.debug("JdbcRatingRepository.create({})", item);
        try {
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(item));
            return findById(item.getId()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create rating:", ex);
            throw new TechnicalException("Failed to create rating", ex);
        }
    }

    @Override
    public Page<Rating> findByReferenceIdAndReferenceTypePageable(String referenceId, RatingReferenceType referenceType, Pageable page) throws TechnicalException {
        LOGGER.debug("JdbcRatingRepository.findByReferenceIdAndReferenceTypePageable({}, {}, {})", referenceId, referenceType, page);
        final List<Rating> ratings;
        try {
            ratings = jdbcTemplate.query("select r.* from ratings r where reference_id = ? and reference_type = ? order by created_at desc"
                    , ORM.getRowMapper()
                    , referenceId
                    , referenceType.name()
            );
        } catch (final Exception ex) {
            final String message = "Failed to find ratings by api pageable";
            LOGGER.error(message, ex);
            throw new TechnicalException(message, ex);
        }
        return getResultAsPage(page, ratings);

    }
    
    @Override
    public List<Rating> findByReferenceIdAndReferenceType(String referenceId, RatingReferenceType referenceType) throws TechnicalException {
        LOGGER.debug("JdbcRatingRepository.findByReferenceIdAndReferenceType({}, {})", referenceId, referenceType);
        try {
            return jdbcTemplate.query("select r.* from ratings r where reference_id = ? and reference_type = ? "
                    , ORM.getRowMapper()
                    , referenceId
                    , referenceType.name()
            );
        } catch (final Exception ex) {
            LOGGER.error("Failed to find ratings by ref:", ex);
            throw new TechnicalException("Failed to find ratings by ref", ex);
        }
    }

    @Override
    public Optional<Rating> findByReferenceIdAndReferenceTypeAndUser(String referenceId, RatingReferenceType referenceType, String user) throws TechnicalException {
        LOGGER.debug("JdbcRatingRepository.findByReferenceIdAndReferenceTypeAndUser({}, {}, {})", referenceId, referenceType, user);
        try {
            List<Rating> ratings = jdbcTemplate.query("select r.* from ratings r where reference_id = ? and reference_type = ?  and " + escapeReservedWord("user") + " = ?"
                    , ORM.getRowMapper()
                    , referenceId
                    , referenceType.name()
                    , user
            );
            return ratings.stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find ratings by api:", ex);
            throw new TechnicalException("Failed to find ratings by api", ex);
        }
    }

}