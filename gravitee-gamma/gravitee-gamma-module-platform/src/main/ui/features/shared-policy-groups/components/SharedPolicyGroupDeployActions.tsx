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

import { Button } from '@gravitee/graphene-core';
import { CloudIcon, CloudUploadIcon } from '@gravitee/graphene-core/icons';

import { useSharedPolicyGroupDeployActions } from '../hooks/useSharedPolicyGroupDeployActions';
import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';

type SharedPolicyGroupDeployActionsProps = Readonly<{
    sharedPolicyGroup: SharedPolicyGroup;
    /** When Policy Studio lands, pass dirty state so Deploy stays blocked until save. */
    hasUnsavedChanges?: boolean;
}>;

/** Classic Console studio header Deploy / Undeploy controls. */
export function SharedPolicyGroupDeployActions({
    sharedPolicyGroup,
    hasUnsavedChanges = false,
}: SharedPolicyGroupDeployActionsProps) {
    const { visible, deployDisabled, undeployDisabled, isDeploying, isUndeploying, onDeploy, onUndeploy } =
        useSharedPolicyGroupDeployActions(sharedPolicyGroup, hasUnsavedChanges);

    if (!visible) {
        return null;
    }

    return (
        <div className="flex shrink-0 flex-wrap items-center gap-2" data-testid="shared-policy-group-deploy-actions">
            <Button
                type="button"
                variant="outline"
                size="sm"
                className="gap-1.5"
                onClick={onUndeploy}
                disabled={undeployDisabled}
                title="Undeploy"
                data-testid="shared-policy-group-undeploy"
            >
                <CloudIcon className="size-4" aria-hidden />
                {isUndeploying ? 'Undeploying…' : 'Undeploy'}
            </Button>
            <Button
                type="button"
                size="sm"
                className="gap-1.5"
                onClick={onDeploy}
                disabled={deployDisabled}
                title="Deploy"
                data-testid="shared-policy-group-deploy"
            >
                <CloudUploadIcon className="size-4" aria-hidden />
                {isDeploying ? 'Deploying…' : 'Deploy'}
            </Button>
        </div>
    );
}
