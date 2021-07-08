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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.media.api.MediaRepository;
import io.gravitee.repository.media.model.Media;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

/**
 * @author Guillaume GILLON
 */
@Repository
public class JdbcMediaRepository extends JdbcAbstractRepository<Media> implements MediaRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcMediaRepository.class);

    JdbcMediaRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "media");
    }

    @Override
    protected JdbcObjectMapper<Media> buildOrm() {
        return JdbcObjectMapper
            .builder(Media.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("type", Types.NVARCHAR, String.class)
            .addColumn("sub_type", Types.NVARCHAR, String.class)
            .addColumn("file_name", Types.NVARCHAR, String.class)
            .addColumn("size", Types.INTEGER, Long.class)
            .addColumn("data", Types.BLOB, byte[].class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("api", Types.NVARCHAR, String.class)
            .addColumn("hash", Types.NVARCHAR, String.class)
            .build();
    }

    @Override
    public List<Media> findAllByApi(String api) {
        LOGGER.debug("JdbcMediaRepository.findAllByApi({})", api);

        String sql = getOrm().getSelectAllSql() + " where api = ?";
        Object[] param = new Object[] { api };

        List<Media> mediaList = jdbcTemplate.query(sql, getOrm().getRowMapper(), param);

        return mediaList.stream().collect(Collectors.toList());
    }

    @Override
    public Media create(Media media) throws TechnicalException {
        LOGGER.debug("JdbcMediaRepository.create({})", media);

        media.setCreatedAt(new Date());

        try {
            Connection conn = DataSourceUtils.getConnection(jdbcTemplate.getDataSource());
            conn.setAutoCommit(false);
            PreparedStatement ps = getOrm().buildInsertPreparedStatementCreator(media).createPreparedStatement(conn);

            ps.execute();
            conn.commit();

            return media;
        } catch (final Exception ex) {
            LOGGER.error("Failed to create media", ex);
            throw new TechnicalException("Failed to create media", ex);
        }
    }

    @Override
    public void deleteAllByApi(String api) {
        LOGGER.debug("JdbcMediaRepository.deleteByApi({})", api);
        jdbcTemplate.update("delete from " + this.tableName + " where api = ?", api);
    }

    @Override
    public Optional<Media> findByHash(String hash) {
        LOGGER.debug("JdbcMediaRepository.findByHash({})", hash);
        return this.findByHashAndApiAndType(hash, null, null, true);
    }

    @Override
    public Optional<Media> findByHash(String hash, boolean withContent) {
        LOGGER.debug("JdbcMediaRepository.findByHash({})", hash);

        return this.findByHashAndApiAndType(hash, null, null, withContent);
    }

    @Override
    public Optional<Media> findByHashAndApi(String hash, String api) {
        LOGGER.debug("JdbcMediaRepository.findByHash({},{})", hash, api);

        return this.findByHashAndApiAndType(hash, api, null, true);
    }

    @Override
    public Optional<Media> findByHashAndApi(String hash, String api, boolean withContent) {
        LOGGER.debug("JdbcMediaRepository.findByHash({},{}, {})", hash, api, withContent);

        return this.findByHashAndApiAndType(hash, api, null, withContent);
    }

    @Override
    public Optional<Media> findByHashAndType(String hash, String mediaType) {
        LOGGER.debug("JdbcMediaRepository.findByHashAndType({},{})", hash, mediaType);

        return this.findByHashAndApiAndType(hash, null, mediaType, true);
    }

    @Override
    public Optional<Media> findByHashAndType(String hash, String mediaType, boolean withContent) {
        LOGGER.debug("JdbcMediaRepository.findByHashAndType({},{},{})", hash, mediaType, withContent);

        return this.findByHashAndApiAndType(hash, null, mediaType, withContent);
    }

    @Override
    public Optional<Media> findByHashAndApiAndType(String hash, String api, String mediaType) {
        LOGGER.debug("JdbcMediaRepository.findByHashAndType({},{},{})", hash, api, mediaType);
        return this.findByHashAndApiAndType(hash, api, mediaType, true);
    }

    @Override
    public Optional<Media> findByHashAndApiAndType(String hash, String api, String mediaType, boolean withContent) {
        LOGGER.debug("JdbcMediaRepository.findByHashAndType({},{},{}, {})", hash, mediaType, api, withContent);

        String select = "select id, type, sub_type, file_name, size, created_at, api, hash";
        if (withContent) {
            select += ", data";
        }
        String sql = select + " from " + escapeReservedWord(this.tableName) + " where hash = ?";
        List<Object> paramList = new ArrayList<>();
        paramList.add(hash);
        Object[] param;
        if (api != null) {
            sql += " and api = ?";
            paramList.add(api);
        }
        if (mediaType != null) {
            sql += " and type = ?";
            paramList.add(mediaType);
        }

        List<Media> mediaList = jdbcTemplate.query(sql, getOrm().getRowMapper(), paramList.toArray());

        return mediaList.stream().findFirst();
    }
}
