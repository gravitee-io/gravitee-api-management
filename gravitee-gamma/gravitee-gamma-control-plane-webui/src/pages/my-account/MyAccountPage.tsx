/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { Alert, AlertDescription, Button } from '@gravitee/graphene-core';

import { DangerZoneCard } from './components/DangerZoneCard';
import { PersonalAccessTokensCard } from './components/PersonalAccessTokensCard';
import { UserInformationCard } from './components/UserInformationCard';
import { currentUserAvatarUrl } from './myAccount.mapping';
import { useMyAccount } from './useMyAccount';
import { useAvatarCacheBust } from '../../features/auth';
import { useEnvironmentStore } from '../../features/environment/environment.store';
import { useBootstrapStore } from '../../shared/config/bootstrap.store';

export function MyAccountPage() {
    const {
        user,
        draft,
        setDraft,
        fieldDefs,
        consoleAuth,
        loading,
        error,
        reload,
        dirty,
        internal,
        saving,
        deleting,
        save,
        cancel,
        deleteAccount,
    } = useMyAccount();
    const environments = useEnvironmentStore(s => s.environments);
    const environmentId = useEnvironmentStore(s => s.environmentId);
    const config = useBootstrapStore(s => s.config);
    const cacheBust = useAvatarCacheBust();

    if (loading && !user) {
        return (
            <div className="space-y-6" data-testid="my-account-page">
                <h1 className="text-2xl font-bold tracking-tight">My Account</h1>
                <p className="text-sm text-muted-foreground">Loading your account…</p>
            </div>
        );
    }

    if (error || !user || !draft) {
        return (
            <div className="space-y-6" data-testid="my-account-page">
                <h1 className="text-2xl font-bold tracking-tight">My Account</h1>
                <Alert>
                    <AlertDescription className="flex items-center justify-between gap-4">
                        <span role="alert">{error?.message ?? 'Failed to load your account.'}</span>
                        <Button type="button" variant="outline" size="sm" onClick={reload}>
                            Retry
                        </Button>
                    </AlertDescription>
                </Alert>
            </div>
        );
    }

    const avatarUrl =
        user.id && config ? currentUserAvatarUrl(config.managementBaseURL, config.organizationId, user.id, cacheBust) : undefined;
    const avatarPreview = draft.resetToDefault ? undefined : (draft.pictureDataUrl ?? avatarUrl);

    return (
        <div className="space-y-6" data-testid="my-account-page">
            <div className="space-y-1">
                <h1 className="text-2xl font-bold tracking-tight">My Account</h1>
                <p className="text-sm text-muted-foreground">Manage your profile, personal access tokens, and account.</p>
            </div>
            <UserInformationCard
                user={user}
                draft={draft}
                fieldDefs={fieldDefs}
                environments={environments}
                avatarPreview={avatarPreview}
                internal={internal}
                dirty={dirty}
                saving={saving}
                onDraftChange={setDraft}
                onSave={() => void save()}
                onCancel={cancel}
            />
            <PersonalAccessTokensCard environmentId={environmentId} />
            <DangerZoneCard
                consoleAuth={consoleAuth}
                primaryOwner={Boolean(user.primaryOwner)}
                displayName={user.displayName}
                deleting={deleting}
                onDelete={() => void deleteAccount()}
            />
        </div>
    );
}
