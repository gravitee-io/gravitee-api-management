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
import io.gravitee.repository.management.api.ViewRepository;
import io.gravitee.repository.management.model.View;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.Date;

/**
 *
 * @author njt
 */
@Repository
public class JdbcViewRepository extends JdbcAbstractCrudRepository<View, String> implements ViewRepository {

    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(View.class, "views", "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("default_view", Types.BIT, boolean.class)
            .addColumn("hidden", Types.BIT, boolean.class)
            .addColumn("order", Types.INTEGER, int.class)
            .addColumn("highlight_api", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
    }

    @Override
    protected String getId(View item) {
        return item.getId();
    }

}
