/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc.mgmt;

import com.groupgti.shared.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.ViewRepository;
import io.gravitee.repository.management.model.View;
import java.sql.Types;
import java.util.Date;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcViewRepository extends JdbcAbstractCrudRepository<View, String> implements ViewRepository {

    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcViewRepository.class);
    
    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(View.class, "Id")
            .addColumn("Id", Types.NVARCHAR, String.class)
            .addColumn("Name", Types.NVARCHAR, String.class)
            .addColumn("Description", Types.NVARCHAR, String.class)
            .addColumn("DefaultView", Types.BIT, boolean.class)
            .addColumn("Hidden", Types.BIT, boolean.class)
            .addColumn("Order", Types.INTEGER, int.class)
            .addColumn("CreatedAt", Types.TIMESTAMP, Date.class)
            .addColumn("UpdatedAt", Types.TIMESTAMP, Date.class)
            .build();    
    
    
    public JdbcViewRepository(DataSource dataSource) {
        super(dataSource, View.class);
    }

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
    }

    @Override
    protected String getId(View item) {
        return item.getId();
    }

}
