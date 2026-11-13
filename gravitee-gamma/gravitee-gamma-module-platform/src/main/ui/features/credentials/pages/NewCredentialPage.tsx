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
import { Button, Card, CardContent, PageFocused, PageHeader } from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { useHasEnvironmentPermission } from '../../../shared/hooks/useEnvironmentPermissions';
import { notify } from '../../../shared/notify';
import { CredentialAuthenticationFields } from '../components/CredentialAuthenticationFields';
import { CredentialExpiryField } from '../components/CredentialExpiryField';
import { CredentialNameAndServiceFields } from '../components/CredentialNameAndServiceFields';
import { CredentialStepMarker } from '../components/CredentialStepMarker';
import { CredentialTypeField } from '../components/CredentialTypeField';
import { useCreateCredential } from '../hooks/useCredentialMutations';
import type { CredentialStrategy } from '../types/credential';
import {
    emptyCredentialDraft,
    toCreateCredentialPayload,
    validateCredentialDraft,
    withoutEditedFieldErrors,
    type CredentialDraft,
    type CredentialDraftErrors,
} from '../utils/credentialDraft';
import { credentialFailureMessage } from '../utils/credentialErrors';
import { ENVIRONMENT_SECRET_CREATE_PERMISSION } from '../utils/credentialPermissions';
import { credentialStrategyLabel } from '../utils/credentialStrategies';

type Step = 'details' | 'authentication';

export function NewCredentialPage() {
    const navigate = useNavigate();
    const canStoreValues = useHasEnvironmentPermission([ENVIRONMENT_SECRET_CREATE_PERMISSION]);
    const createMutation = useCreateCredential();
    const [step, setStep] = useState<Step>('details');
    const [draft, setDraft] = useState<CredentialDraft>(() => ({
        ...emptyCredentialDraft(),
        mode: canStoreValues ? 'value' : 'reference',
    }));
    const [errors, setErrors] = useState<CredentialDraftErrors>({});

    function updateDraft(next: CredentialDraft) {
        setErrors(current => withoutEditedFieldErrors(current, draft, next));
        setDraft(next);
    }

    function changeStrategy(strategy: CredentialStrategy) {
        setDraft(previous => ({ ...previous, strategy, settings: {}, secrets: {} }));
    }

    function goToAuthentication() {
        const detailErrors = validateCredentialDraft(draft, { secretsRequired: false, now: new Date() });
        if (detailErrors.name) {
            setErrors({ name: detailErrors.name });
            return;
        }
        setErrors({});
        setStep('authentication');
    }

    async function create() {
        const draftErrors = validateCredentialDraft(draft, { secretsRequired: true, now: new Date() });
        setErrors(draftErrors);
        if (Object.keys(draftErrors).length > 0) {
            return;
        }
        try {
            const created = await createMutation.mutateAsync(toCreateCredentialPayload(draft));
            notify.success('Credential created');
            navigate(`../${created.id}`);
        } catch (error) {
            notify.error(credentialFailureMessage(error, 'The credential could not be created.'));
        }
    }

    return (
        <PageFocused>
            <div className="flex flex-col gap-6">
                <div>
                    <Button type="button" variant="ghost" size="sm" onClick={() => navigate('..')}>
                        <ArrowLeftIcon aria-hidden />
                        Back to credentials
                    </Button>
                </div>
                <PageHeader title="Add credential" description="Create a credential that modules can reuse to call a service." />

                <ol className="flex items-center gap-3 text-sm" aria-label="Steps">
                    <CredentialStepMarker
                        index={1}
                        label="Credential details"
                        current={step === 'details'}
                        done={step === 'authentication'}
                    />
                    <li aria-hidden className="h-px flex-1 bg-border" />
                    <CredentialStepMarker index={2} label="Authentication" current={step === 'authentication'} done={false} />
                </ol>

                <Card>
                    <CardContent className="space-y-4 pt-6">
                        {step === 'details' ? (
                            <>
                                <CredentialNameAndServiceFields draft={draft} errors={errors} onChange={updateDraft} />
                                <CredentialTypeField value={draft.strategy} onChange={changeStrategy} />
                            </>
                        ) : (
                            <>
                                <div className="space-y-1">
                                    <p className="text-sm font-medium">Authentication type</p>
                                    <p className="text-sm text-muted-foreground">{credentialStrategyLabel(draft.strategy)}</p>
                                </div>
                                <CredentialAuthenticationFields
                                    idPrefix="credential"
                                    draft={draft}
                                    errors={errors}
                                    onChange={updateDraft}
                                    showModeChoice
                                    canStoreValues={canStoreValues}
                                />
                                <CredentialExpiryField
                                    id="credential-expires"
                                    value={draft.expiresAt}
                                    error={errors.expiresAt}
                                    onChange={expiresAt => updateDraft({ ...draft, expiresAt })}
                                />
                            </>
                        )}
                    </CardContent>
                </Card>

                <div className="flex items-center justify-between border-t pt-4">
                    <Button type="button" variant="ghost" onClick={() => navigate('..')}>
                        Cancel
                    </Button>
                    {step === 'details' ? (
                        <Button type="button" onClick={goToAuthentication}>
                            Next
                        </Button>
                    ) : (
                        <div className="flex gap-2">
                            <Button type="button" variant="outline" onClick={() => setStep('details')}>
                                <ArrowLeftIcon aria-hidden />
                                Back
                            </Button>
                            <Button type="button" disabled={createMutation.isPending} onClick={() => void create()}>
                                {createMutation.isPending ? 'Creating…' : 'Create credential'}
                            </Button>
                        </div>
                    )}
                </div>
            </div>
        </PageFocused>
    );
}
