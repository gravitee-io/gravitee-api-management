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
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.model.Parameter;
import org.springframework.stereotype.Repository;

import java.sql.Types;

/**
 *
 * @author njt
 */
@Repository
public class JdbcParameterRepository extends JdbcAbstractCrudRepository<Parameter, String> implements ParameterRepository {

    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Parameter.class, "parameters", "key")
            .addColumn("key", Types.NVARCHAR, String.class)
            .addColumn("value", Types.NVARCHAR, String.class)
            .build();

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
    }

    @Override
    protected String getId(Parameter item) {
        return item.getKey();
    }
}