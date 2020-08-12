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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.media.api.MediaRepository;
import io.gravitee.repository.media.model.Media;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Guillaume GILLON
 */
@Repository
public class JdbcMediaRepository implements MediaRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcMediaRepository.class);

    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Media.class, "media", "id")
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

    @Override
    public Optional<Media> findByHash(String hash, String mediaType) {
        LOGGER.debug("JdbcMediaRepository.findMediaBy({},{})", hash, mediaType);
        return this.findByHashAndApi(hash, null, mediaType);
    }

    @Override
    public List<Media> findAllByApi(String api) {
        LOGGER.debug("JdbcMediaRepository.findAllByApi({})", api);

        String sql = "select * from media where api = ?";
        Object[] param = new Object[]{api};

        List<Media> mediaList = jdbcTemplate.query(sql, ORM.getRowMapper(), param);

        return mediaList.stream().collect(Collectors.toList());
    }

    @Override
    public Media create(Media media) throws TechnicalException {
        LOGGER.debug("JdbcMediaRepository.create({})", media);

        media.setCreatedAt(new Date());

        try {
            Connection conn = DataSourceUtils.getConnection(jdbcTemplate.getDataSource());
            conn.setAutoCommit(false);
            PreparedStatement ps = ORM.buildInsertPreparedStatementCreator(media).createPreparedStatement(conn);

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
        jdbcTemplate.update("delete from media where api = ?", api);
    }

    @Override
    public Optional<Media> findByHashAndApi(String hash, String api, String mediaType) {
        LOGGER.debug("JdbcMediaRepository.findByHashAndApi({},{},{})", hash, mediaType, api);

        String sql;
        Object[] param;
        if (api != null) {
            sql = "select * from media where hash = ? and type = ? and api = ?";
            param = new Object[]{hash, mediaType, api};
        } else {
            sql = "select * from media where hash = ? and type = ?";
            param = new Object[]{hash, mediaType};
        }

        List<Media> mediaList = jdbcTemplate.query(sql,
            ORM.getRowMapper(),
            param);

        return mediaList.stream().findFirst();
    }
}
