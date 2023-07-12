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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

/**
 * @author Guillaume GILLON
 * @author GraviteeSource Team
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
    public List<Media> findAllByApi(String api) throws TechnicalException {
        LOGGER.debug("JdbcMediaRepository.findAllByApi({})", api);

        if (api == null) {
            LOGGER.warn("Returning an empty list because API identifier given as an argument is null");
            return List.of();
        } else {
            return doFindAllByApi(api);
        }
    }

    private List<Media> doFindAllByApi(String api) throws TechnicalException {
        try {
            String sql = getOrm().getSelectAllSql() + " where api = ?";
            Object[] param = new Object[] { api };
            List<Media> mediaList = jdbcTemplate.query(sql, getOrm().getRowMapper(), param);
            return new ArrayList<>(mediaList);
        } catch (Exception e) {
            throw new TechnicalException(e);
        }
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
    public void deleteAllByApi(String api) throws TechnicalException {
        LOGGER.debug("JdbcMediaRepository.deleteByApi({})", api);

        if (api == null) {
            LOGGER.warn("Skipping media deletion because the API identifier given as an argument is null");
        } else {
            doDeleteAllByApi(api);
        }
    }

    private void doDeleteAllByApi(String api) throws TechnicalException {
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where api = ?", api);
        } catch (Exception e) {
            throw new TechnicalException(e);
        }
    }

    @Override
    public void deleteByHashAndApi(String hash, String api) throws TechnicalException {
        LOGGER.debug("JdbcMediaRepository.deleteByHashAndApi({}, {})", hash, api);

        try {
            jdbcTemplate.update("delete from " + this.tableName + " where hash = ? and api = ?", hash, api);
        } catch (Exception e) {
            throw new TechnicalException(e);
        }
    }

    @Override
    public Optional<Media> findByHash(String hash) throws TechnicalException {
        LOGGER.debug("JdbcMediaRepository.findByHash({})", hash);

        try {
            return this.findByHashAndApiAndType(hash, null, null, true);
        } catch (Exception e) {
            throw new TechnicalException(e);
        }
    }

    @Override
    public Optional<Media> findByHash(String hash, boolean withContent) throws TechnicalException {
        LOGGER.debug("JdbcMediaRepository.findByHash({})", hash);

        try {
            return this.findByHashAndApiAndType(hash, null, null, withContent);
        } catch (Exception e) {
            throw new TechnicalException(e);
        }
    }

    @Override
    public Optional<Media> findByHashAndApi(String hash, String api) throws TechnicalException {
        LOGGER.debug("JdbcMediaRepository.findByHash({},{})", hash, api);

        try {
            return this.findByHashAndApiAndType(hash, api, null, true);
        } catch (Exception e) {
            throw new TechnicalException(e);
        }
    }

    @Override
    public Optional<Media> findByHashAndApi(String hash, String api, boolean withContent) throws TechnicalException {
        LOGGER.debug("JdbcMediaRepository.findByHash({},{}, {})", hash, api, withContent);

        try {
            return this.findByHashAndApiAndType(hash, api, null, withContent);
        } catch (Exception e) {
            throw new TechnicalException(e);
        }
    }

    @Override
    public Optional<Media> findByHashAndType(String hash, String mediaType) throws TechnicalException {
        LOGGER.debug("JdbcMediaRepository.findByHashAndType({},{})", hash, mediaType);

        try {
            return this.findByHashAndApiAndType(hash, null, mediaType, true);
        } catch (Exception e) {
            throw new TechnicalException(e);
        }
    }

    @Override
    public Optional<Media> findByHashAndApiAndType(String hash, String api, String mediaType) throws TechnicalException {
        LOGGER.debug("JdbcMediaRepository.findByHashAndType({},{},{})", hash, api, mediaType);

        try {
            return this.findByHashAndApiAndType(hash, api, mediaType, true);
        } catch (Exception e) {
            throw new TechnicalException(e);
        }
    }

    @Override
    public Optional<Media> findByHashAndApiAndType(String hash, String api, String mediaType, boolean withContent)
        throws TechnicalException {
        LOGGER.debug("JdbcMediaRepository.findByHashAndType({},{},{}, {})", hash, mediaType, api, withContent);

        try {
            String select = "select id, type, sub_type, file_name, size, created_at, api, hash";
            if (withContent) {
                select += ", data";
            }
            String sql = select + " from " + escapeReservedWord(this.tableName) + " where hash = ?";
            List<Object> paramList = new ArrayList<>();
            paramList.add(hash);

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
        } catch (Exception e) {
            throw new TechnicalException(e);
        }
    }
}
