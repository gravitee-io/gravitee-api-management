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
package io.gravitee.repository.jdbc.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.RatingAnswerRepository;
import io.gravitee.repository.management.model.RatingAnswer;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcRatingAnswerRepository extends JdbcAbstractCrudRepository<RatingAnswer, String> implements RatingAnswerRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcRatingAnswerRepository.class);

    JdbcRatingAnswerRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "rating_answers");
    }

    @Override
    protected JdbcObjectMapper<RatingAnswer> buildOrm() {
        return JdbcObjectMapper
            .builder(RatingAnswer.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("rating", Types.NVARCHAR, String.class)
            .addColumn("user", Types.NVARCHAR, String.class)
            .addColumn("comment", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    protected String getId(RatingAnswer item) {
        return item.getId();
    }

    @Override
    public RatingAnswer create(RatingAnswer item) throws TechnicalException {
        LOGGER.debug("JdbcRatingAnswerRepository.create({})", item);
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(item));
            return findById(item.getId()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create ratingAnswer:", ex);
            throw new TechnicalException("Failed to create ratingAnswer", ex);
        }
    }

    @Override
    public List<RatingAnswer> findByRating(String rating) throws TechnicalException {
        LOGGER.debug("JdbcRatingAnswerRepository.findByRating({})", rating);
        try {
            return jdbcTemplate.query("select ra.* from " + this.tableName + " ra where rating = ?", getOrm().getRowMapper(), rating);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find rating answers by rating:", ex);
            throw new TechnicalException("Failed to find rating answers by rating", ex);
        }
    }

    @Override
    public List<String> deleteByRating(String ratingId) throws TechnicalException {
        LOGGER.debug("JdbcRatingAnswerRepository.deleteByRatingId({})", ratingId);
        try {
            final var rows = jdbcTemplate.queryForList("select id from " + this.tableName + " where rating = ?", String.class, ratingId);

            if (!rows.isEmpty()) {
                jdbcTemplate.update("delete from " + tableName + " where rating = ?", ratingId);
            }

            return rows;
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete rating answers by ratingId: {}", ratingId, ex);
            throw new TechnicalException("Failed to delete rating answers by ratingId", ex);
        }
    }
}
