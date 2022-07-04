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
package io.gravitee.test.junit.rules;

import java.util.concurrent.TimeUnit;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author GraviteeSource Team
 */
public class FlakyRunner implements TestRule {

    private static final Logger LOG = LoggerFactory.getLogger(FlakyRunner.class);

    private final int retryCount;
    private final int secondsBetweenRetries;

    private RetryableStatement statement;

    public FlakyRunner(int retryCount, int secondsBetweenRetries) {
        this.retryCount = retryCount;
        this.secondsBetweenRetries = secondsBetweenRetries;
    }

    public FlakyRunner() {
        this(2, 3);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        statement = new RetryableStatement(base, retryCount);
        return statement;
    }

    RetryableStatement statement() {
        return statement;
    }

    class RetryableStatement extends Statement {

        final Statement base;
        final int maxRetries;

        int retries;

        RetryableStatement(Statement base, int maxRetries) {
            this.base = base;
            this.maxRetries = maxRetries;
            this.retries = maxRetries;
        }

        @Override
        public void evaluate() throws Throwable {
            try {
                base.evaluate();
            } catch (Throwable t) {
                if (--retries < 1) {
                    throw new FlakyTestError("Test failed after " + maxRetries + "retries");
                }
                LOG.warn("Retrying failing test after {} seconds with {} attempts", secondsBetweenRetries, retries);
                TimeUnit.SECONDS.sleep(secondsBetweenRetries);
                evaluate();
            }
        }

        public int retries() {
            return retries;
        }
    }
}
