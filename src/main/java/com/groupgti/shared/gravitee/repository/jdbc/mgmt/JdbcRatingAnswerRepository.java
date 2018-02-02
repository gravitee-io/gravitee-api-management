/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc.mgmt;

import com.groupgti.shared.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RatingAnswerRepository;
import io.gravitee.repository.management.model.RatingAnswer;
import java.sql.Types;
import java.util.Date;
import java.util.List;
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
public class JdbcRatingAnswerRepository extends JdbcAbstractCrudRepository<RatingAnswer, String> implements RatingAnswerRepository {

    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcRatingAnswerRepository.class);
    
    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(RatingAnswer.class, "Id")
            .addColumn("Id", Types.NVARCHAR, String.class)
            .addColumn("Rating", Types.NVARCHAR, String.class)
            .addColumn("User", Types.NVARCHAR, String.class)
            .addColumn("Comment", Types.NVARCHAR, String.class)
            .addColumn("CreatedAt", Types.TIMESTAMP, Date.class)
            .addColumn("UpdatedAt", Types.TIMESTAMP, Date.class)
            .build(); 
        
    @Autowired
    public JdbcRatingAnswerRepository(DataSource dataSource) {
        super(dataSource, RatingAnswer.class);
    }

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
    }

    @Override
    protected String getId(RatingAnswer item) {
        return item.getId();
    }

    @Override
    public RatingAnswer create(RatingAnswer item) throws TechnicalException {
        logger.debug("JdbcRatingAnswerRepository.create({})", item);
        try {
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(item));
            return findById(item.getId()).get();
        } catch (Throwable ex) {
            logger.error("Failed to create ratingAnswer:", ex);
            throw new TechnicalException("Failed to create ratingAnswer", ex);
        }
    }

    @Override
    public List<RatingAnswer> findByRating(String rating) throws TechnicalException {
        
        logger.debug("JdbcRatingAnswerRepository.findByRating({})", rating);
        try {
            
            List<RatingAnswer> ras = jdbcTemplate.query("select distinct ra.* from RatingAnswer ra where Rating = ?"
                    , ORM.getRowMapper()
                    , rating
            );
            return ras;
        } catch (Throwable ex) {
            logger.error("Failed to find rating answers by rating:", ex);
            throw new TechnicalException("Failed to find rating answers by rating", ex);
        }
        
    }

}
