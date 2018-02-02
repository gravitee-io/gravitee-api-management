/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc.mgmt;

import com.groupgti.shared.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;

/**
 *
 * @author njt
 */
public abstract class JdbcAbstractCrudRepository<T, ID> {
    
    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcAbstractCrudRepository.class);
    
    protected final JdbcTemplate jdbcTemplate;
    private final String tableName;

    @Autowired
    public JdbcAbstractCrudRepository(DataSource dataSource, Class<T> clazz) {
        logger.debug("JdbcAbstractCrudRepository({})", dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.tableName = clazz.getSimpleName();
    }
    
    protected abstract JdbcObjectMapper getOrm();
    protected abstract ID getId(T item);
    
    protected RowMapper<T> getRowMapper() {
        return getOrm().getRowMapper();
    }
    
    public Optional<T> findById(ID id) throws TechnicalException {

        logger.debug("JdbcAbstractCrudRepository<{}>.findById({})", tableName, id);
        
        try {
            List<T> items = jdbcTemplate.query(getOrm().getSelectByIdSql()
                    , getRowMapper()
                    , id
            );
            return items.stream().findFirst();
        } catch (Throwable ex) {
            logger.error("Failed to find {} items by id:", tableName, ex);
            throw new TechnicalException("Failed to find " + tableName + " items by id", ex);
        }
        
    }
    
    public Set<T> findAll() throws TechnicalException {

        logger.debug("JdbcAbstractCrudRepository<{}>.findAll()", tableName);
        
        try {
            List<T> items = jdbcTemplate.query(getOrm().getSelectAllSql()
                    , getRowMapper()
            );
            return new HashSet<>(items);
        } catch (Throwable ex) {
            logger.error("Failed to find all {} items:", tableName, ex);
            throw new TechnicalException("Failed to find all " + tableName + " items", ex);
        }
        
    }
    
    public T create(T item) throws TechnicalException {
        
        logger.debug("JdbcAbstractCrudRepository<{}>.create({})", tableName, item);
        try {
            jdbcTemplate.update(buildInsertPreparedStatementCreator(item));
            return findById(getId(item)).get();
        } catch (Throwable ex) {
            logger.error("Failed to create {} item:", tableName, ex);
            throw new TechnicalException("Failed to create " + tableName + " item", ex);
        }

    }

    protected PreparedStatementCreator buildInsertPreparedStatementCreator(T item) {
        return getOrm().buildInsertPreparedStatementCreator(item);
    }

    public T update(T item) throws TechnicalException {
        
        logger.debug("JdbcAbstractCrudRepository<{}>.update({})", tableName, item);
        
        if (item == null) {
            throw new IllegalStateException("Unable to update null item");
        }
        
        try {
            int rows = jdbcTemplate.update(buildUpdatePreparedStatementCreator(item));
            if (rows == 0) {
                throw new IllegalStateException("Unable to update " + item.getClass().getSimpleName() + " " + getId(item));
            } else {
                return findById(getId(item)).get();
            }
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Throwable ex) {
            logger.error("Failed to update {} item:", tableName, ex);
            throw new TechnicalException("Failed to update " + tableName + " item", ex);
        }

    }

    protected PreparedStatementCreator buildUpdatePreparedStatementCreator(T item) {
        return getOrm().buildUpdatePreparedStatementCreator(item, getId(item));
    }
    
    public void delete(ID id) throws TechnicalException {
        
        logger.debug("JdbcAbstractCrudRepository<{}>.delete({})", tableName, id);
        try {
            jdbcTemplate.update(getOrm().getDeleteSql(), id);
        } catch (Throwable ex) {
            logger.error("Failed to delete {} item:", tableName, ex);
            throw new TechnicalException("Failed to delete " + tableName + " item", ex);
        }
        
    }
}