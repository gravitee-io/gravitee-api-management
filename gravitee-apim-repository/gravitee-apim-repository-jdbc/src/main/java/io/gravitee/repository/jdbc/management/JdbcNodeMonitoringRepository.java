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

import io.gravitee.node.api.Monitoring;
import io.gravitee.node.api.NodeMonitoringRepository;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.internal.operators.maybe.MaybeJust;
import io.reactivex.schedulers.Schedulers;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcNodeMonitoringRepository extends JdbcAbstractRepository<Monitoring> implements NodeMonitoringRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcNodeMonitoringRepository.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    JdbcNodeMonitoringRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "node_monitoring");
    }

    @Override
    protected JdbcObjectMapper<Monitoring> buildOrm() {
        return JdbcObjectMapper
            .builder(Monitoring.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("node_id", Types.NVARCHAR, String.class)
            .addColumn("type", Types.NVARCHAR, String.class)
            .addColumn("payload", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("evaluated_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    public Maybe<Monitoring> findByNodeIdAndType(String nodeId, String type) {
        LOGGER.debug("JdbcNodeMonitoringRepository.findByNodeIdAndType({}, {})", nodeId, type);
        return Maybe
            .<Monitoring>create(
                emitter -> {
                    try {
                        List<Monitoring> monitoringEvents = jdbcTemplate.query(
                            getOrm().getSelectAllSql() + " where node_id = ? and type = ?",
                            getOrm().getRowMapper(),
                            nodeId,
                            type
                        );

                        if (monitoringEvents.isEmpty()) {
                            emitter.onComplete();
                        } else {
                            emitter.onSuccess(monitoringEvents.get(0));
                        }
                    } catch (final Exception ex) {
                        LOGGER.error("Failed to find node monitoring by node_id and type:", ex);
                        emitter.onError(new TechnicalException("Failed to find node monitoring by node_id and type", ex));
                    }
                }
            )
            .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<Monitoring> create(Monitoring monitoring) {
        LOGGER.debug("JdbcNodeMonitoringRepository.create({})", monitoring);
        return Single
            .<Monitoring>create(
                emitter -> {
                    try {
                        jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(monitoring));

                        findById(monitoring.getId())
                            .ifPresentOrElse(
                                emitter::onSuccess,
                                () -> emitter.onError(new TechnicalException("Failed to create node monitoring"))
                            );
                    } catch (final Exception ex) {
                        LOGGER.error("Failed to create node monitoring:", ex);
                        emitter.onError(new TechnicalException("Failed to create node monitoring", ex));
                    }
                }
            )
            .subscribeOn(Schedulers.io());
    }

    public Optional<Monitoring> findById(String id) throws TechnicalException {
        LOGGER.debug("JdbcNodeMonitoringRepository<{}>.findById({})", getOrm().getTableName(), id);
        try {
            List<Monitoring> items = jdbcTemplate.query(getOrm().getSelectByIdSql(), getRowMapper(), id);
            return items.stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find {} items by id:", getOrm().getTableName(), ex);
            throw new TechnicalException("Failed to find " + getOrm().getTableName() + " items by id", ex);
        }
    }

    @Override
    public Single<Monitoring> update(Monitoring monitoring) {
        LOGGER.debug("JdbcNodeMonitoringRepository.update({})", monitoring);
        return Single
            .<Monitoring>create(
                emitter -> {
                    try {
                        jdbcTemplate.update(getOrm().buildUpdatePreparedStatementCreator(monitoring, monitoring.getId()));

                        findById(monitoring.getId())
                            .ifPresentOrElse(
                                emitter::onSuccess,
                                () ->
                                    emitter.onError(
                                        new IllegalStateException(
                                            String.format("No node monitoring found with id [%s]", monitoring.getId())
                                        )
                                    )
                            );
                    } catch (final Exception ex) {
                        LOGGER.error("Failed to update node monitoring:", ex);
                        emitter.onError(new TechnicalException("Failed to update node monitoring", ex));
                    }
                }
            )
            .subscribeOn(Schedulers.io());
    }

    @Override
    public Flowable<Monitoring> findByTypeAndTimeFrame(String type, long from, long to) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("JdbcNodeMonitoringRepository.findByTypeAndTimeFrame({}, {}, {})", type, from, to);
        }

        final List<Object> args = new ArrayList<>();
        final StringBuilder builder = new StringBuilder("select nm.* from " + this.tableName + " nm ");
        builder.append("where type = ? and updated_at >= ?");
        args.add(type);
        args.add(new Date(from));

        if (to > from) {
            builder.append(" and updated_at <= ?");
            args.add(new Date(to));
        }

        return Flowable
            .fromIterable(
                jdbcTemplate.query(
                    (Connection cnctn) -> {
                        PreparedStatement stmt = cnctn.prepareStatement(builder.toString());
                        int idx = 1;
                        for (final Object arg : args) {
                            if (arg instanceof Date) {
                                final Date date = (Date) arg;
                                stmt.setTimestamp(idx++, new Timestamp(date.getTime()));
                            } else {
                                stmt.setObject(idx++, arg);
                            }
                        }
                        return stmt;
                    },
                    getRowMapper()
                )
            )
            .subscribeOn(Schedulers.io());
    }
}
