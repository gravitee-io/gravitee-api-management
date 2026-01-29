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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.ClientCertificateRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ClientCertificate;
import io.gravitee.repository.management.model.ClientCertificateStatus;
import java.sql.Types;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation of the ClientCertificateRepository.
 *
 * @author GraviteeSource Team
 */
@Repository
public class JdbcClientCertificateRepository
    extends JdbcAbstractCrudRepository<ClientCertificate, String>
    implements ClientCertificateRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcClientCertificateRepository.class);

    JdbcClientCertificateRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "client_certificates");
    }

    @Override
    protected JdbcObjectMapper<ClientCertificate> buildOrm() {
        return JdbcObjectMapper.builder(ClientCertificate.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("cross_id", Types.NVARCHAR, String.class)
            .addColumn("application_id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("starts_at", Types.TIMESTAMP, Date.class)
            .addColumn("ends_at", Types.TIMESTAMP, Date.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .addColumn("certificate", Types.NCLOB, String.class)
            .addColumn("certificate_expiration", Types.TIMESTAMP, Date.class)
            .addColumn("subject", Types.NVARCHAR, String.class)
            .addColumn("issuer", Types.NVARCHAR, String.class)
            .addColumn("fingerprint", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("organization_id", Types.NVARCHAR, String.class)
            .addColumn("status", Types.NVARCHAR, ClientCertificateStatus.class)
            .build();
    }

    @Override
    protected String getId(ClientCertificate item) {
        return item.getId();
    }

    @Override
    public Page<ClientCertificate> findByApplicationId(String applicationId, Pageable pageable) throws TechnicalException {
        LOGGER.debug("JdbcClientCertificateRepository.findByApplicationId({})", applicationId);
        try {
            String sql = getOrm().getSelectAllSql() + " WHERE application_id = ?";
            List<ClientCertificate> clientCertificates = jdbcTemplate.query(sql, getOrm().getRowMapper(), applicationId);
            return getResultAsPage(pageable, clientCertificates);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find client certificates by application id", ex);
        }
    }

    @Override
    public Set<ClientCertificate> findByApplicationIdAndStatuses(String applicationId, Collection<ClientCertificateStatus> statuses)
        throws TechnicalException {
        LOGGER.debug("JdbcClientCertificateRepository.findByApplicationIdAndStatuses({}, {})", applicationId, statuses);

        if (statuses == null || statuses.isEmpty()) {
            return new HashSet<>();
        }

        try {
            List<String> statusStrings = statuses.stream().map(ClientCertificateStatus::name).toList();
            String statusPlaceholders = statusStrings
                .stream()
                .map(s -> "?")
                .collect(Collectors.joining(", "));

            String sql = getOrm().getSelectAllSql() + " WHERE application_id = ? AND status IN (" + statusPlaceholders + ")";

            var args = Stream.concat(Stream.of(applicationId), statusStrings.stream()).toArray();

            List<ClientCertificate> clientCertificates = jdbcTemplate.query(sql, getOrm().getRowMapper(), args);
            return new HashSet<>(clientCertificates);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find client certificates by application id and statuses", ex);
        }
    }

    @Override
    public boolean existsByFingerprintAndActiveApplication(String fingerprint, String environmentId) throws TechnicalException {
        LOGGER.debug("JdbcClientCertificateRepository.existsByFingerprintAndActiveApplication({}, {})", fingerprint, environmentId);

        try {
            String sql =
                "SELECT 1 FROM " +
                tableName +
                " cc INNER JOIN " +
                getTableNameFor("applications") +
                " a ON cc.application_id = a.id " +
                "WHERE cc.fingerprint = ? AND cc.environment_id = ? AND cc.status != ? AND a.status = ?";

            List<Integer> results = jdbcTemplate.queryForList(
                sql,
                Integer.class,
                fingerprint,
                environmentId,
                ClientCertificateStatus.REVOKED.name(),
                ApplicationStatus.ACTIVE.name()
            );

            return !results.isEmpty();
        } catch (Exception ex) {
            throw new TechnicalException("Failed to check if client certificate exists for active application", ex);
        }
    }
}
