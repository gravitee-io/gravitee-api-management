/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc.mgmt;

import com.groupgti.shared.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MetadataRepository;
import java.sql.Types;
import java.util.Date;
import io.gravitee.repository.management.model.Metadata;
import io.gravitee.repository.management.model.MetadataFormat;
import io.gravitee.repository.management.model.MetadataReferenceType;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcMetadataRepository implements MetadataRepository {

    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcMetadataRepository.class);
    
    private final JdbcTemplate jdbcTemplate;
    
    private static final JdbcObjectMapper ORM = 
            JdbcObjectMapper.builder(Metadata.class, "Key")
                    .updateSql("update Metadata set "
                            + " `Key` = ?"
                            + " , `ReferenceType` = ?"
                            + " , `ReferenceId` = ?"
                            + " , `Name` = ?"
                            + " , `Format` = ?"
                            + " , `Value` = ?"
                            + " , `CreatedAt` = ? "
                            + " , `UpdatedAt` = ? "
                            + " where "
                            + "   `Key` = ? "
                            + "   and ReferenceType = ? "
                            + "   and ReferenceId = ? "
                    )
                    .addColumn("Key", Types.NVARCHAR, String.class)
                    .addColumn("ReferenceType", Types.NVARCHAR, MetadataReferenceType.class)
                    .addColumn("ReferenceId", Types.NVARCHAR, String.class)
                    .addColumn("Name", Types.NVARCHAR, String.class)
                    .addColumn("Format", Types.NVARCHAR, MetadataFormat.class)
                    .addColumn("Value", Types.NVARCHAR, String.class)
                    .addColumn("CreatedAt", Types.TIMESTAMP, Date.class)
                    .addColumn("UpdatedAt", Types.TIMESTAMP, Date.class)
                    .build();    
    
    @Autowired
    public JdbcMetadataRepository(DataSource dataSource) {
        logger.debug("JdbcMetadataRepository({})", dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Metadata create(Metadata metadata) throws TechnicalException {
        
        logger.debug("JdbcMetadataRepository.create({})", metadata);
        try {
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(metadata));
            return findById(metadata.getKey(), metadata.getReferenceId(), metadata.getReferenceType()).get();
        } catch (Throwable ex) {
            logger.error("Failed to create metadata:", ex);
            throw new TechnicalException("Failed to create metadata", ex);
        }
        
    }

    @Override
    public Metadata update(Metadata metadata) throws TechnicalException {
        
        logger.debug("JdbcMetadataRepository.update({})", metadata);
        if (metadata == null) {
            throw new IllegalStateException("Failed to update null");
        }
        try {
            jdbcTemplate.update(ORM.buildUpdatePreparedStatementCreator(metadata
                    , metadata.getKey()
                    , metadata.getReferenceType().name()
                    , metadata.getReferenceId()
            ));
            return findById(metadata.getKey(), metadata.getReferenceId(), metadata.getReferenceType()).get();
        } catch (NoSuchElementException ex) {
            logger.error("Failed to update api:", ex);
            throw new IllegalStateException("Failed to update api", ex);
        } catch (Throwable ex) {
            logger.error("Failed to update metadata:", ex);
            throw new TechnicalException("Failed to update metadata", ex);
        }
        
    }

    @Override
    public void delete(String key, String referenceId, MetadataReferenceType referenceType) throws TechnicalException {
        logger.debug("JdbcMetadataRepository.delete({}, {}, {})", key, referenceId, referenceType);
        try {
            jdbcTemplate.update("delete from Metadata where `Key` = ? and ReferenceType = ? and ReferenceId = ? "
                    , key
                    , referenceType.name()
                    , referenceId
            );
        } catch (Throwable ex) {
            logger.error("Failed to create item:", ex);
            throw new TechnicalException("Failed to create item", ex);
        }
    }

    @Override
    public Optional<Metadata> findById(String key, String referenceId, MetadataReferenceType referenceType) throws TechnicalException {

        logger.debug("JdbcMetadataRepository.findById({}, {}, {})", key, referenceId, referenceType);
        
        try {
            List<Metadata> items = jdbcTemplate.query("select * from Metadata where `Key` = ? and ReferenceType = ? and ReferenceId = ?"
                    , ORM.getRowMapper()
                    , key
                    , referenceType.name()
                    , referenceId
            );
            return items.stream().findFirst();
        } catch (Throwable ex) {
            logger.error("Failed to find metadata by id:", ex);
            throw new TechnicalException("Failed to find metadata by id", ex);
        }
        
    }

    @Override
    public List<Metadata> findByKeyAndReferenceType(String key, MetadataReferenceType referenceType) throws TechnicalException {

        logger.debug("JdbcMetadataRepository.findByKeyAndReferenceType({}, {})", key, referenceType);
        
        try {
            List<Metadata> items = jdbcTemplate.query("select * from Metadata where `Key` = ? and ReferenceType = ?"
                    , ORM.getRowMapper()
                    , key
                    , referenceType.name()
            );
            return items;
        } catch (Throwable ex) {
            logger.error("Failed to find metadata by key and reference type:", ex);
            throw new TechnicalException("Failed to find metadata by key and reference type", ex);
        }
        
    }

    @Override
    public List<Metadata> findByReferenceType(MetadataReferenceType referenceType) throws TechnicalException {

        logger.debug("JdbcMetadataRepository.findByReferenceType({}, {})", referenceType);
        
        try {
            List<Metadata> items = jdbcTemplate.query("select * from Metadata where ReferenceType = ?"
                    , ORM.getRowMapper()
                    , referenceType.name()
            );
            return items;
        } catch (Throwable ex) {
            logger.error("Failed to find metadata by reference type:", ex);
            throw new TechnicalException("Failed to find metadata by reference type", ex);
        }
        
    }

    @Override
    public List<Metadata> findByReferenceTypeAndReferenceId(MetadataReferenceType referenceType, String referenceId) throws TechnicalException {

        logger.debug("JdbcMetadataRepository.findById({}, {})", referenceId, referenceType);
        
        try {
            List<Metadata> items = jdbcTemplate.query("select * from Metadata where ReferenceType = ? and ReferenceId = ?"
                    , ORM.getRowMapper()
                    , referenceType.name()
                    , referenceId
            );
            return items;
        } catch (Throwable ex) {
            logger.error("Failed to find metadata by reference type and reference id:", ex);
            throw new TechnicalException("Failed to find metadata by reference type and reference id", ex);
        }
        
    }

}
