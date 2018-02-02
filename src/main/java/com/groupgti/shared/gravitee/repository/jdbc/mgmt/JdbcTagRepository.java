/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc.mgmt;

import com.groupgti.shared.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.TagRepository;
import io.gravitee.repository.management.model.Tag;
import java.sql.Types;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcTagRepository extends JdbcAbstractCrudRepository<Tag, String> implements TagRepository {

    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcTagRepository.class);
    
    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Tag.class, "Id")
            .addColumn("Id", Types.NVARCHAR, String.class)
            .addColumn("Name", Types.NVARCHAR, String.class)
            .addColumn("Description", Types.NVARCHAR, String.class)
            .build();    

    
    public JdbcTagRepository(DataSource dataSource) {
        super(dataSource, Tag.class);
    }

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
    }

    @Override
    protected String getId(Tag item) {
        return item.getId();
    }

}
