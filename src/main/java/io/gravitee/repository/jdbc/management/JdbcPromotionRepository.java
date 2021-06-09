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

import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.PromotionRepository;
import io.gravitee.repository.management.model.Promotion;

import io.gravitee.repository.management.model.PromotionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.Date;

@Repository
public class JdbcPromotionRepository extends JdbcAbstractCrudRepository<Promotion, String> implements PromotionRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcPromotionRepository.class);

    JdbcPromotionRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "promotions");
    }

    @Override
    protected JdbcObjectMapper<Promotion> buildOrm() {
        return JdbcObjectMapper.builder(Promotion.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("api_definition", Types.NCLOB, String.class)
            .addColumn("status", Types.NVARCHAR, PromotionStatus.class)
            .addColumn("target_environment_id", Types.NVARCHAR, String.class)
            .addColumn("target_installation_id", Types.NVARCHAR, String.class)
            .addColumn("source_environment_id", Types.NVARCHAR, String.class)
            .addColumn("source_installation_id", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    protected String getId(Promotion item) {
        return item.getId();
    }
}