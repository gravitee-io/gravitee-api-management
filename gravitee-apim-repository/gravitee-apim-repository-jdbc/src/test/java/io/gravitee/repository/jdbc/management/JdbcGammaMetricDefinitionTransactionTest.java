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
import io.gravitee.repository.management.model.GammaMetricDefinition;
import jakarta.inject.Inject;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The Gamma save use case runs in one transaction and reads the record back when its insert loses a race on the key.
 * Transactions are a JDBC concern, hence a JDBC specific test.
 *
 * @author GraviteeSource Team
 */
public class JdbcGammaMetricDefinitionTransactionTest extends AbstractManagementRepositoryTest {

    private static final String ENVIRONMENT = "env-1";
    private static final String RAW_KEY = "double_checkout_basket-value";

    @Inject
    private PlatformTransactionManager graviteeTransactionManager;

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @Test
    public void should_find_the_winner_in_the_same_transaction_after_a_duplicate_insert() throws Exception {
        // On PostgreSQL a failed statement aborts the whole transaction: the read that follows would fail too.
        var winner = gammaMetricDefinitionRepository.create(aRecord("winner", "Basket value"));

        Optional<GammaMetricDefinition> found = new TransactionTemplate(graviteeTransactionManager).execute(status -> {
            var failure = catchThrowable(() -> gammaMetricDefinitionRepository.create(aRecord("loser", "Basket")));
            assertThat(failure).isInstanceOf(DuplicateKeyException.class);
            try {
                return gammaMetricDefinitionRepository.findByEnvironmentIdAndRawKey(ENVIRONMENT, RAW_KEY);
            } catch (TechnicalException e) {
                throw new IllegalStateException(e);
            }
        });

        assertThat(found).hasValueSatisfying(record -> {
            assertThat(record.getId()).isEqualTo(winner.getId());
            assertThat(record.getLabel()).isEqualTo("Basket value");
        });
    }

    private static GammaMetricDefinition aRecord(String id, String label) {
        return GammaMetricDefinition.builder()
            .id(id)
            .environmentId(ENVIRONMENT)
            .rawKey(RAW_KEY)
            .label(label)
            .exposed(true)
            .createdAt(new Date(1000000000000L))
            .updatedAt(new Date(1000000000000L))
            .build();
    }
}
