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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.model.Secret;
import jakarta.inject.Inject;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Transactions are a JDBC concern, hence a JDBC specific test. */
public class JdbcSecretTransactionTest extends AbstractManagementRepositoryTest {

    private static final String KEY = "openai-key";

    @Inject
    private PlatformTransactionManager graviteeTransactionManager;

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @Test
    public void should_find_the_winner_in_the_same_transaction_after_a_duplicate_insert() throws Exception {
        secretRepository.create(aSecret("winner"));

        Optional<Secret> found = new TransactionTemplate(graviteeTransactionManager).execute(status -> {
            var failure = catchThrowable(() -> secretRepository.create(aSecret("loser")));
            assertThat(failure).isInstanceOf(DuplicateKeyException.class);
            try {
                return secretRepository.findByKey("org-1", "env-1", KEY);
            } catch (TechnicalException e) {
                throw new IllegalStateException(e);
            }
        });

        assertThat(found).map(Secret::getId).contains("winner");
    }

    private static Secret aSecret(String id) {
        return Secret.builder()
            .id(id)
            .organizationId("org-1")
            .environmentId("env-1")
            .key(KEY)
            .encryptedValue("ciphertext-" + id)
            .createdAt(new Date(1753711200000L))
            .updatedAt(new Date(1753711300000L))
            .build();
    }
}
