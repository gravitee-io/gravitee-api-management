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
package io.gravitee.repository.jdbc.embedded;

import static ru.yandex.qatools.embed.postgresql.distribution.Version.Main.V10;
import static ru.yandex.qatools.embed.postgresql.distribution.Version.Main.V11;

import io.gravitee.repository.jdbc.AbstractJdbcTestRepositoryConfiguration;
import java.io.IOException;
import javax.inject.Inject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres;
import ru.yandex.qatools.embed.postgresql.distribution.Version;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Conditional(PostgreSQLCondition.class)
public class PostgreSQLTestRepositoryConfiguration extends AbstractJdbcTestRepositoryConfiguration {

    private static final String MACOS_M1_ARCH = "aarch64";

    @Inject
    private EmbeddedPostgres embeddedPostgres;

    @Override
    protected String getJdbcUrl() {
        return embeddedPostgres.getConnectionUrl().orElse(null);
    }

    @Bean(destroyMethod = "stop")
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        final EmbeddedPostgres embeddedPostgres = new EmbeddedPostgres(version());
        embeddedPostgres.start("localhost", 5423, "gravitee", "gravitee", "gravitee");
        return embeddedPostgres;
    }

    private static Version.Main version() {
        return MACOS_M1_ARCH.equals(System.getProperty("os.arch")) ? V11 : V10;
    }
}
