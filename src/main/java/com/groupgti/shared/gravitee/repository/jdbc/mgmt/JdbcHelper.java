/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc.mgmt;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;


/**
 *
 * @author njt
 */
public class JdbcHelper {
    
    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcHelper.class);
    
    @FunctionalInterface
    public static interface ChildAdder<T> {
        
        void addChild(T parent, ResultSet rs) throws SQLException;
        
    }
    
    public static class CollatingRowMapper<T> implements RowCallbackHandler {

        private final RowMapper<T> mapper;
        private final ChildAdder<T> childAdder;
        private final String idColumn;
        private final List<T> rows;
        private Comparable lastId;
        private T current;

        public CollatingRowMapper(RowMapper mapper, ChildAdder<T> childAdder, String idColumn) {
            this.mapper = mapper;
            this.childAdder = childAdder;
            this.idColumn = idColumn;
            this.rows = new ArrayList<>();
        }
        
        @Override
        public void processRow(ResultSet rs) throws SQLException {
            
            Comparable currentId = (Comparable)rs.getObject(idColumn);
            if ((lastId == null) || (lastId.compareTo(currentId) != 0)) {
                lastId = currentId;
                current = mapper.mapRow(rs, rows.size() + 1);
                rows.add(current);
            }
            childAdder.addChild(current, rs);
            
        }

        public List<T> getRows() {
            return rows;
        }
            
    }

    public static class CollatingRowMapperTwoColumn<T> implements RowCallbackHandler {

        private final RowMapper<T> mapper;
        private final ChildAdder<T> childAdder;
        private final String idColumn1;
        private final String idColumn2;
        private final List<T> rows;
        private Comparable lastId1;
        private Comparable lastId2;
        private T current;

        public CollatingRowMapperTwoColumn(RowMapper mapper, ChildAdder<T> childAdder, String idColumn1, String idColumn2) {
            this.mapper = mapper;
            this.childAdder = childAdder;
            this.idColumn1 = idColumn1;
            this.idColumn2 = idColumn2;
            this.rows = new ArrayList<>();
        }
        
        @Override
        public void processRow(ResultSet rs) throws SQLException {
            
            Comparable currentId1 = (Comparable)rs.getObject(idColumn1);
            Comparable currentId2 = (Comparable)rs.getObject(idColumn2);
            if (
                    (lastId1 == null) 
                    || (lastId1.compareTo(currentId1) != 0)
                    || (lastId2 == null) 
                    || (lastId2.compareTo(currentId2) != 0)
                    ) {
                lastId1 = currentId1;
                lastId2 = currentId2;
                current = mapper.mapRow(rs, rows.size() + 1);
                rows.add(current);
            }
            childAdder.addChild(current, rs);
        }

        public List<T> getRows() {
            return rows;
        }
            
    }
}
