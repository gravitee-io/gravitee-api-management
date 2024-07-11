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

import static java.lang.String.format;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.SharedPolicyGroupRepository;
import io.gravitee.repository.management.model.SharedPolicyGroup;
import io.gravitee.repository.management.model.SharedPolicyGroupLifecycleState;
import java.sql.Types;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSharedPolicyGroupRepository
    extends JdbcAbstractCrudRepository<SharedPolicyGroup, String>
    implements SharedPolicyGroupRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcSharedPolicyGroupRepository.class);

    JdbcSharedPolicyGroupRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "sharedpolicygroups");
    }

    @Override
    protected JdbcObjectMapper<SharedPolicyGroup> buildOrm() {
        return JdbcObjectMapper
            .builder(SharedPolicyGroup.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("organization_id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("cross_id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("version", Types.INTEGER, Integer.class)
            .addColumn("api_type", Types.NVARCHAR, ApiType.class)
            .addColumn("definition", Types.NVARCHAR, String.class)
            .addColumn("lifecycle_state", Types.NVARCHAR, SharedPolicyGroupLifecycleState.class)
            .addColumn("deployed_at", Types.TIMESTAMP, java.util.Date.class)
            .addColumn("created_at", Types.TIMESTAMP, java.util.Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, java.util.Date.class)
            .build();
    }

    @Override
    protected String getId(SharedPolicyGroup item) {
        return item.getId();
    }

    @Override
    public SharedPolicyGroup create(SharedPolicyGroup item) throws TechnicalException {
        return super.create(item);
    }

    @Override
    public SharedPolicyGroup update(final SharedPolicyGroup item) throws TechnicalException {
        return super.update(item);
    }

    @Override
    public void delete(String id) throws TechnicalException {
        super.delete(id);
    }

    @Override
    public Optional<SharedPolicyGroup> findById(String id) throws TechnicalException {
        return super.findById(id);
    }

    @Override
    public Set<SharedPolicyGroup> findAll() throws TechnicalException {
        throw new IllegalStateException("Not implemented");
    }
}
