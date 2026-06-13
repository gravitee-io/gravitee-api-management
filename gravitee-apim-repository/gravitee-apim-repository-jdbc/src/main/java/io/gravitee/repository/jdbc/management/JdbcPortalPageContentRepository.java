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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.PortalPageContentRepository;
import io.gravitee.repository.management.model.AutomationTargetReferenceType;
import io.gravitee.repository.management.model.PortalPageContent;
import io.gravitee.repository.management.model.PortalPageContent.AutomationMetadata;
import java.sql.Types;
import java.util.List;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
@CustomLog
public class JdbcPortalPageContentRepository
    extends JdbcAbstractCrudRepository<PortalPageContent, String>
    implements PortalPageContentRepository {

    private static final ObjectMapper JSON = new ObjectMapper();

    JdbcPortalPageContentRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "portal_page_contents");
    }

    @Override
    protected JdbcObjectMapper<PortalPageContent> buildOrm() {
        return JdbcObjectMapper.builder(PortalPageContent.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("type", Types.NVARCHAR, PortalPageContent.Type.class)
            .addColumn("configuration", Types.NVARCHAR, String.class)
            .addColumn("content", Types.NVARCHAR, String.class)
            .addColumn("organization_id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn(
                "automation_metadata",
                Types.NCLOB,
                AutomationMetadata.class,
                JdbcPortalPageContentRepository::serialize,
                JdbcPortalPageContentRepository::deserialize
            )
            .addMirroredColumn("automation_reference_type", Types.NVARCHAR, item -> {
                var meta = item.getAutomationMetadata();
                return meta != null && meta.getReferenceType() != null ? meta.getReferenceType().name() : null;
            })
            .addMirroredColumn("automation_reference_id", Types.NVARCHAR, item ->
                item.getAutomationMetadata() != null ? item.getAutomationMetadata().getReferenceId() : null
            )
            .build();
    }

    @Override
    public List<PortalPageContent> findAllByType(PortalPageContent.Type type) throws TechnicalException {
        log.debug("JdbcPortalPageContentRepository.findAllByType({})", type);
        try {
            return jdbcTemplate.query(getOrm().getSelectAllSql() + " WHERE type = ?", getOrm().getRowMapper(), type.name());
        } catch (Exception ex) {
            log.error("Failed to find portal page contents by type", ex);
            throw new TechnicalException("Failed to find portal page contents by type", ex);
        }
    }

    @Override
    protected String getId(PortalPageContent item) {
        return item.getId();
    }

    @Override
    public List<PortalPageContent> findByAutomationReference(
        String environmentId,
        AutomationTargetReferenceType referenceType,
        String referenceId
    ) throws TechnicalException {
        log.debug("JdbcPortalPageContentRepository.findByAutomationReference({}, {}, {})", environmentId, referenceType, referenceId);
        try {
            return jdbcTemplate.query(
                getOrm().getSelectAllSql() + " WHERE environment_id = ? AND automation_reference_type = ? AND automation_reference_id = ?",
                getOrm().getRowMapper(),
                environmentId,
                referenceType.name(),
                referenceId
            );
        } catch (Exception ex) {
            log.error("Failed to find portal page contents by automation reference", ex);
            throw new TechnicalException("Failed to find portal page contents by automation reference", ex);
        }
    }

    private static String serialize(AutomationMetadata meta) {
        try {
            return JSON.writeValueAsString(meta);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize automation metadata", e);
        }
    }

    private static AutomationMetadata deserialize(String json) {
        try {
            return JSON.readValue(json, AutomationMetadata.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to deserialize automation metadata", e);
        }
    }
}
