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

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.RatingRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.RatingCriteria;
import io.gravitee.repository.management.model.Rating;
import io.gravitee.repository.management.model.RatingReferenceType;
import io.gravitee.repository.management.model.RatingSummary;
import java.sql.Types;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcRatingRepository extends JdbcAbstractCrudRepository<Rating, String> implements RatingRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcRatingRepository.class);

    JdbcRatingRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "ratings");
    }

    @Override
    protected JdbcObjectMapper<Rating> buildOrm() {
        return JdbcObjectMapper
            .builder(Rating.class, this.tableName, "id")
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
    }

    @Override
    protected String getId(Rating item) {
        return item.getId();
    }

    @Override
    public Rating create(Rating item) throws TechnicalException {
        LOGGER.debug("JdbcRatingRepository.create({})", item);
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(item));
            return findById(item.getId()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create rating:", ex);
            throw new TechnicalException("Failed to create rating", ex);
        }
    }

    @Override
    public Page<Rating> findByReferenceIdAndReferenceTypePageable(String referenceId, RatingReferenceType referenceType, Pageable page)
        throws TechnicalException {
        LOGGER.debug("JdbcRatingRepository.findByReferenceIdAndReferenceTypePageable({}, {}, {})", referenceId, referenceType, page);
        final List<Rating> ratings;
        try {
            ratings =
                jdbcTemplate.query(
                    "select r.* from " + this.tableName + " r where reference_id = ? and reference_type = ? order by created_at desc",
                    getOrm().getRowMapper(),
                    referenceId,
                    referenceType.name()
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
            return jdbcTemplate.query(
                "select r.* from " + this.tableName + " r where reference_id = ? and reference_type = ? ",
                getOrm().getRowMapper(),
                referenceId,
                referenceType.name()
            );
        } catch (final Exception ex) {
            LOGGER.error("Failed to find ratings by ref:", ex);
            throw new TechnicalException("Failed to find ratings by ref", ex);
        }
    }

    @Override
    public Optional<Rating> findByReferenceIdAndReferenceTypeAndUser(String referenceId, RatingReferenceType referenceType, String user)
        throws TechnicalException {
        LOGGER.debug("JdbcRatingRepository.findByReferenceIdAndReferenceTypeAndUser({}, {}, {})", referenceId, referenceType, user);
        try {
            List<Rating> ratings = jdbcTemplate.query(
                "select r.* from " +
                this.tableName +
                " r where reference_id = ? and reference_type = ?  and " +
                escapeReservedWord("user") +
                " = ?",
                getOrm().getRowMapper(),
                referenceId,
                referenceType.name(),
                user
            );
            return ratings.stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find ratings by api:", ex);
            throw new TechnicalException("Failed to find ratings by api", ex);
        }
    }

    @Override
    public Map<String, RatingSummary> findSummariesByCriteria(RatingCriteria ratingCriteria) throws TechnicalException {
        LOGGER.debug("JdbcRatingRepository.findSummariesByCriteria({})", ratingCriteria.toString());
        try {
            boolean hasInClause = ratingCriteria.getReferenceIds() != null && !ratingCriteria.getReferenceIds().isEmpty();

            return jdbcTemplate.query(
                this.buildSummariesQuery(ratingCriteria, hasInClause),
                this.buildSummariesSetter(ratingCriteria, hasInClause),
                resultSet -> {
                    Map<String, RatingSummary> ratingSummaries = new LinkedHashMap<>();
                    while (resultSet.next()) {
                        String referenceId = resultSet.getString(1);
                        RatingSummary ratingSummary = new RatingSummary();
                        ratingSummary.setApi(referenceId);
                        ratingSummary.setAverageRate(resultSet.getDouble(2));
                        ratingSummary.setNumberOfRatings(resultSet.getInt(3));
                        ratingSummaries.put(referenceId, ratingSummary);
                    }
                    return ratingSummaries;
                }
            );
        } catch (final Exception ex) {
            LOGGER.error("Failed to find rating summaries by criteria:", ex);
            throw new TechnicalException("Failed to find rating summaries by criteria", ex);
        }
    }

    private PreparedStatementSetter buildSummariesSetter(RatingCriteria ratingCriteria, boolean hasInClause) {
        return ps -> {
            ps.setString(1, ratingCriteria.getReferenceType().name());
            ps.setInt(2, ratingCriteria.getGt());
            if (hasInClause) {
                getOrm().setArguments(ps, ratingCriteria.getReferenceIds(), 3);
            }
        };
    }

    private String buildSummariesQuery(RatingCriteria ratingCriteria, boolean hasInClause) {
        StringBuilder queryBuilder = new StringBuilder(
            "SELECT reference_id, avg(cast(rate as decimal)) as averageRate, count(rate) as numberOfRatings, max(updated_at) as lastUpdatedAt from "
        )
            .append(this.tableName)
            .append(" where reference_type = ? and rate > ? ");

        if (hasInClause) {
            queryBuilder.append("and reference_id IN (").append(getOrm().buildInClause(ratingCriteria.getReferenceIds())).append(") ");
        }
        queryBuilder.append("group by reference_id order by averageRate desc, numberOfRatings desc, lastUpdatedAt desc");

        return queryBuilder.toString();
    }

    @Override
    public Set<String> computeRanking(RatingCriteria ratingCriteria) throws TechnicalException {
        LOGGER.debug("JdbcRatingRepository.computeRankingByCriteria({})", ratingCriteria.toString());
        try {
            boolean hasInClause = ratingCriteria.getReferenceIds() != null && !ratingCriteria.getReferenceIds().isEmpty();

            return jdbcTemplate.query(
                this.buildSummariesQuery(ratingCriteria, hasInClause),
                this.buildSummariesSetter(ratingCriteria, hasInClause),
                resultSet -> {
                    Set<String> ranking = new LinkedHashSet<>();
                    while (resultSet.next()) {
                        String referenceId = resultSet.getString(1);
                        ranking.add(referenceId);
                    }
                    return ranking;
                }
            );
        } catch (final Exception ex) {
            LOGGER.error("Failed to compute ranking by criteria:", ex);
            throw new TechnicalException("Failed to compute ranking by criteria", ex);
        }
    }
}
