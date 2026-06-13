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
package io.gravitee.repository.jdbc.orm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class JdbcObjectMapperTest {

    // ─── Minimal test entity ──────────────────────────────────────────────────

    public static class Item {

        private String id;
        private String name;
        private Nested nested;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Nested getNested() {
            return nested;
        }

        public void setNested(Nested nested) {
            this.nested = nested;
        }
    }

    public static class Nested {

        private String value;

        public Nested() {}

        public Nested(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // ─── Mapper setup ─────────────────────────────────────────────────────────

    private JdbcObjectMapper<Item> mapper;

    @BeforeEach
    void setUp() {
        mapper = JdbcObjectMapper.builder(Item.class, "items", "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            // "nested" maps to getNested()/setNested(Nested); ser/deser handle JSON encoding
            .addColumn(
                "nested",
                Types.NCLOB,
                Nested.class,
                n -> "json:" + n.getValue(),
                json -> new Nested(json.substring("json:".length()))
            )
            .addMirroredColumn("nested_value", Types.NVARCHAR, item -> item.getNested() != null ? item.getNested().getValue() : null)
            .build();
    }

    // ─── SQL generation ───────────────────────────────────────────────────────

    @Test
    void insert_sql_contains_all_column_names() throws Exception {
        String sql = captureInsertSql();

        assertTrue(sql.contains("nested"), "INSERT SQL should include custom-serialized column");
        assertTrue(sql.contains("nested_value"), "INSERT SQL should include mirrored column");
    }

    @Test
    void update_sql_contains_all_column_names() throws Exception {
        String sql = captureUpdateSql();

        assertTrue(sql.contains("nested"), "UPDATE SQL should include custom-serialized column");
        assertTrue(sql.contains("nested_value"), "UPDATE SQL should include mirrored column");
    }

    @Test
    void insert_sql_has_correct_placeholder_count() throws Exception {
        String sql = captureInsertSql();
        // id, name, nested, nested_value = 4 placeholders
        long count = sql
            .chars()
            .filter(c -> c == '?')
            .count();
        assertEquals(4, count);
    }

    // ─── Write behaviour ──────────────────────────────────────────────────────

    @Test
    void custom_column_serializer_is_called_on_insert() throws Exception {
        var item = itemWithNested("hello");
        var ps = mockInsert(item);

        // position 3 = nested (after id=1, name=2)
        verify(ps).setString(3, "json:hello");
    }

    @Test
    void mirrored_column_is_written_on_insert() throws Exception {
        var item = itemWithNested("hello");
        var ps = mockInsert(item);

        // position 4 = nested_value (after id=1, name=2, nested=3)
        verify(ps).setString(4, "hello");
    }

    @Test
    void custom_column_null_sets_null_on_insert() throws Exception {
        var item = new Item();
        item.setId("id1");
        var ps = mockInsert(item);

        verify(ps).setNull(3, Types.CLOB);
    }

    @Test
    void mirrored_column_null_sets_null_on_insert() throws Exception {
        var item = new Item();
        item.setId("id1");
        var ps = mockInsert(item);

        verify(ps).setNull(4, Types.VARCHAR);
    }

    // ─── Read behaviour ───────────────────────────────────────────────────────

    @Test
    void custom_column_deserializer_is_called_on_row_mapping() throws Exception {
        var rs = mockResultSet("id1", "myName", "json:world");

        Item item = mapper.getRowMapper().mapRow(rs, 0);

        assertNotNull(item.getNested());
        assertEquals("world", item.getNested().getValue());
    }

    @Test
    void base_columns_are_populated_on_row_mapping() throws Exception {
        var rs = mockResultSet("id1", "myName", "json:world");

        Item item = mapper.getRowMapper().mapRow(rs, 0);

        assertEquals("id1", item.getId());
        assertEquals("myName", item.getName());
    }

    @Test
    void null_custom_column_leaves_nested_null() throws Exception {
        var rs = mockResultSet("id1", "myName", null);

        Item item = mapper.getRowMapper().mapRow(rs, 0);

        assertNull(item.getNested());
    }

    @Test
    void mirrored_column_does_not_affect_entity_on_row_mapping() throws Exception {
        var rs = mockResultSet("id1", "myName", "json:world");
        when(rs.getString("nested_value")).thenReturn("ignored");

        Item item = mapper.getRowMapper().mapRow(rs, 0);

        // nested comes from the "nested" column, not nested_value
        assertEquals("world", item.getNested().getValue());
        verify(rs, never()).getObject("nested_value");
        verify(rs, never()).getString("nested_value");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Item itemWithNested(String value) {
        var item = new Item();
        item.setId("id1");
        item.setName("test");
        item.setNested(new Nested(value));
        return item;
    }

    private PreparedStatement mockInsert(Item item) throws Exception {
        var con = mock(Connection.class);
        var ps = mock(PreparedStatement.class);
        when(con.prepareStatement(anyString())).thenReturn(ps);
        mapper.buildInsertPreparedStatementCreator(item).createPreparedStatement(con);
        return ps;
    }

    private String captureInsertSql() throws Exception {
        var con = mock(Connection.class);
        var ps = mock(PreparedStatement.class);
        var captor = ArgumentCaptor.forClass(String.class);
        when(con.prepareStatement(captor.capture())).thenReturn(ps);
        mapper.buildInsertPreparedStatementCreator(new Item()).createPreparedStatement(con);
        return captor.getValue();
    }

    private String captureUpdateSql() throws Exception {
        var con = mock(Connection.class);
        var ps = mock(PreparedStatement.class);
        var captor = ArgumentCaptor.forClass(String.class);
        when(con.prepareStatement(captor.capture())).thenReturn(ps);
        mapper.buildUpdatePreparedStatementCreator(new Item(), "id1").createPreparedStatement(con);
        return captor.getValue();
    }

    private ResultSet mockResultSet(String id, String name, String nestedJson) throws Exception {
        var rs = mock(ResultSet.class);
        when(rs.getObject("id")).thenReturn(id);
        when(rs.getObject("name")).thenReturn(name);
        when(rs.getObject("nested")).thenReturn(nestedJson);
        when(rs.wasNull()).thenReturn(false);
        return rs;
    }
}
