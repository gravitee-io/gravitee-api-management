/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc.mgmt;

import com.groupgti.shared.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RatingRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Rating;
import java.sql.Types;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcRatingRepository extends JdbcAbstractCrudRepository<Rating, String> implements RatingRepository {

    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcRatingRepository.class);
    
    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Rating.class, "Id")
            .addColumn("Id", Types.NVARCHAR, String.class)
            .addColumn("Api", Types.NVARCHAR, String.class)
            .addColumn("User", Types.NVARCHAR, String.class)
            .addColumn("Rate", Types.TINYINT, byte.class)
            .addColumn("Title", Types.NVARCHAR, String.class)
            .addColumn("Comment", Types.NVARCHAR, String.class)
            .addColumn("CreatedAt", Types.TIMESTAMP, Date.class)
            .addColumn("UpdatedAt", Types.TIMESTAMP, Date.class)
            .build(); 
        
    @Autowired
    public JdbcRatingRepository(DataSource dataSource) {
        super(dataSource, Rating.class);
    }

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
        logger.debug("JdbcRatingRepository.create({})", item);
        try {
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(item));
            return findById(item.getId()).get();
        } catch (Throwable ex) {
            logger.error("Failed to create rating:", ex);
            throw new TechnicalException("Failed to create rating", ex);
        }
    }

    @Override
    public Page<Rating> findByApiPageable(String api, Pageable page) throws TechnicalException {
                
        logger.debug("JdbcRatingRepository.findByApiPageable({}, {})", api, page);
        
        List<Rating> ratings = null;
        try {
            
            ratings = jdbcTemplate.query("select distinct r.* from Rating r where Api = ?"
                    , ORM.getRowMapper()
                    , api
            );

        } catch (Throwable ex) {
            logger.error("Failed to find ratings by api:", ex);
            throw new TechnicalException("Failed to find ratings by api", ex);
        }        
        
        if (page != null) {
            int start = page.from();
            if ((start == 0) && (page.pageNumber() > 0)) {
                start = page.pageNumber() * page.pageSize();
            }
            int rows = page.pageSize();
            if ((rows == 0) && (page.to() > 0)) {
                rows = page.to() - start;
            }
            if (start + rows > ratings.size()) {
                rows = ratings.size() - start;
            }

            if (rows > 0) {
                return new Page(ratings.subList(start, start + rows), start / page.pageSize(), rows, ratings.size());
            } else {
                return new Page(Collections.EMPTY_LIST, 0, 0, ratings.size());
            }
        }
        return new Page(ratings, 0, ratings.size(), ratings.size());
        
    }

    @Override
    public List<Rating> findByApi(String apiId) throws TechnicalException {

        logger.debug("JdbcRatingRepository.findByApi({})", apiId);
        try {
            
            List<Rating> ratings = jdbcTemplate.query("select distinct r.* from Rating r where Api = ?"
                    , ORM.getRowMapper()
                    , apiId
            );
            return ratings;
        } catch (Throwable ex) {
            logger.error("Failed to find ratings by api:", ex);
            throw new TechnicalException("Failed to find ratings by api", ex);
        }
        
    }

    @Override
    public Optional<Rating> findByApiAndUser(String api, String user) throws TechnicalException {
        
        logger.debug("JdbcRatingRepository.findByApiAndUser({}, {})", api, user);
        try {
            
            List<Rating> ratings = jdbcTemplate.query("select distinct r.* from Rating r where Api = ? and User = ?"
                    , ORM.getRowMapper()
                    , api
                    , user
            );
            return ratings.stream().findFirst();
        } catch (Throwable ex) {
            logger.error("Failed to find ratings by api:", ex);
            throw new TechnicalException("Failed to find ratings by api", ex);
        }
        
    }
    
}
