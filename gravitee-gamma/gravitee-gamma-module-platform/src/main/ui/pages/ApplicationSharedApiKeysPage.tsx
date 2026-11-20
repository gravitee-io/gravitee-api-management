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
import { Skeleton } from '@gravitee/graphene-core';

import { ApplicationSharedApiKeysView } from '../features/applications/components/subscriptions/ApplicationSharedApiKeysView';
import { useApplicationDetailContext } from '../features/applications/context/ApplicationDetailContext';
import {
    useApplicationSharedApiKeys,
    useRenewApplicationApiKey,
    useRevokeApplicationApiKey,
} from '../features/applications/hooks/useApplicationSharedApiKeys';
import { useApplicationSubscriptionPermissions } from '../features/applications/hooks/useApplicationSubscriptionPermissions';
import { useEnvironmentPortalConfiguration } from '../features/applications/hooks/useEnvironmentPortalConfiguration';
import { isCustomApiKeyEnabled } from '../features/applications/services/environmentPortal';
import { notify } from '../shared/notify';

export function ApplicationSharedApiKeysPage() {
    const { application, isLoading } = useApplicationDetailContext();
    // Classic application shared keys: application-subscription-u gates renew and revoke.
    // Renew stays available when every key is revoked or expired.
    const { canUpdate, permissionsReady } = useApplicationSubscriptionPermissions();
    const portal = useEnvironmentPortalConfiguration();
    const sharedMode = application?.api_key_mode === 'SHARED';
    const keysQuery = useApplicationSharedApiKeys(application?.id, sharedMode);
    const renewMutation = useRenewApplicationApiKey(application?.id);
    const revokeMutation = useRevokeApplicationApiKey(application?.id);

    if (isLoading) {
        return (
            <div className="space-y-6">
                <Skeleton className="h-10 w-64" />
                <Skeleton className="h-96 w-full" />
            </div>
        );
    }

    if (!application) {
        return <p className="text-sm text-muted-foreground">Application not found or you may not have access.</p>;
    }

    const handleRenew = async (customApiKey?: string) => {
        try {
            await renewMutation.mutateAsync(customApiKey);
            notify.success('API Key renewed');
        } catch (error) {
            notify.error(error, 'Failed to renew API key. Please try again.');
            throw error;
        }
    };

    const handleRevoke = async (apiKeyId: string) => {
        try {
            await revokeMutation.mutateAsync(apiKeyId);
            notify.success('API Key revoked');
        } catch (error) {
            notify.error(error, 'Failed to revoke API key. Please try again.');
            throw error;
        }
    };

    return (
        <ApplicationSharedApiKeysView
            apiKeyMode={application.api_key_mode}
            apiKeys={keysQuery.data ?? []}
            isLoading={keysQuery.isLoading}
            isError={keysQuery.isError}
            canManage={permissionsReady && application.status !== 'ARCHIVED' && canUpdate}
            canUseCustomApiKey={!portal.isLoading && isCustomApiKeyEnabled(portal.data)}
            renewPending={renewMutation.isPending}
            revokePending={revokeMutation.isPending}
            onRenew={handleRenew}
            onRevoke={handleRevoke}
        />
    );
}
