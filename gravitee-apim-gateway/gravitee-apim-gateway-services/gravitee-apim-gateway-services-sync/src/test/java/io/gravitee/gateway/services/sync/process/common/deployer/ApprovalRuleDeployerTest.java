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
package io.gravitee.gateway.services.sync.process.common.deployer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import io.gravitee.gateway.handlers.api.manager.ApprovalRuleManager;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalRule;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalRule.ArgumentCondition;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalRule.ToolBinding;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.approvalrule.ApprovalRuleDeployable;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApprovalRuleDeployerTest {

    private static final List<ArgumentCondition> CONDITIONS = List.of(new ArgumentCondition("amount", "GT", "1000"));
    private static final List<ToolBinding> BINDINGS = List.of(new ToolBinding("api-1", "transfer"));
    private static final List<String> ENTITY_IDS = List.of("payments.refund");

    @Mock
    private ApprovalRuleManager approvalRuleManager;

    private ApprovalRuleDeployer cut;

    @BeforeEach
    void beforeEach() {
        cut = new ApprovalRuleDeployer(approvalRuleManager);
    }

    @Nested
    class DeployTest {

        @Test
        void should_deploy_approval_rule() {
            cut.deploy(deployable(SyncAction.DEPLOY)).test().assertComplete();

            verify(approvalRuleManager).deploy(
                new DeployedApprovalRule(
                    "approval-rule-1",
                    "env-1",
                    "org-1",
                    "payments.*",
                    BINDINGS,
                    ENTITY_IDS,
                    CONDITIONS,
                    10,
                    1000L,
                    2000L
                )
            );
        }

        @Test
        void should_return_error_when_approval_rule_manager_throws_exception() {
            doThrow(new RuntimeException("error")).when(approvalRuleManager).deploy(any());

            cut.deploy(deployable(SyncAction.DEPLOY)).test().assertFailure(SyncException.class);
        }
    }

    @Nested
    class UndeployTest {

        @Test
        void should_undeploy_approval_rule() {
            cut.undeploy(deployable(SyncAction.UNDEPLOY)).test().assertComplete();

            verify(approvalRuleManager).undeploy("env-1", "approval-rule-1");
        }

        @Test
        void should_return_error_when_approval_rule_manager_throws_exception() {
            doThrow(new RuntimeException("error")).when(approvalRuleManager).undeploy(any(), any());

            cut.undeploy(deployable(SyncAction.UNDEPLOY)).test().assertFailure(SyncException.class);
        }
    }

    private static ApprovalRuleDeployable deployable(SyncAction syncAction) {
        return ApprovalRuleDeployable.builder()
            .approvalRuleId("approval-rule-1")
            .environmentId("env-1")
            .organizationId("org-1")
            .toolPattern("payments.*")
            .toolBindings(BINDINGS)
            .toolEntityIds(ENTITY_IDS)
            .argumentConditions(CONDITIONS)
            .priority(10)
            .createdAt(1000L)
            .updatedAt(2000L)
            .syncAction(syncAction)
            .build();
    }
}
