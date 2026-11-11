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
import {
    Button,
    Card,
    CardContent,
    Field,
    FieldDescription,
    FieldError,
    FieldLabel,
    Input,
    PageFocused,
    ToggleGroup,
    ToggleGroupItem,
} from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { useHasEnvironmentPermission } from '../../../shared/hooks/useEnvironmentPermissions';
import { notify } from '../../../shared/notify';
import { CredentialAuthenticationFields, CredentialExpiryField } from '../components/CredentialAuthenticationFields';
import { useCreateCredential } from '../hooks/useCredentialMutations';
import { CREDENTIAL_STRATEGIES, type CredentialStrategy } from '../types/credential';
import {
    emptyCredentialDraft,
    toCreateCredentialPayload,
    validateCredentialDraft,
    withoutEditedFieldErrors,
    type CredentialDraft,
    type CredentialDraftErrors,
} from '../utils/credentialDraft';
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
            notify.error(error, 'The credential could not be created.');
        }
    }

    return (
        <PageFocused>
            <div className="mx-auto max-w-3xl space-y-6">
                <div className="space-y-2">
                    <Button type="button" variant="ghost" className="gap-1.5 px-0 text-muted-foreground" onClick={() => navigate('..')}>
                        <ArrowLeftIcon className="size-4" aria-hidden />
                        Back to credentials
                    </Button>
                    <h1 className="text-2xl font-semibold tracking-tight">Add credential</h1>
                    <p className="text-sm text-muted-foreground">Create a credential that modules can reuse to call a service.</p>
                </div>

                <ol className="flex items-center gap-3 text-sm" aria-label="Steps">
                    <StepMarker index={1} label="Credential details" current={step === 'details'} done={step === 'authentication'} />
                    <li aria-hidden className="h-px flex-1 bg-border" />
                    <StepMarker index={2} label="Authentication" current={step === 'authentication'} done={false} />
                </ol>

                <Card>
                    <CardContent className="space-y-4 pt-6">
                        {step === 'details' ? (
                            <>
                                <Field orientation="vertical" className="gap-1.5">
                                    <FieldLabel htmlFor="credential-name" required>
                                        Name
                                    </FieldLabel>
                                    <Input
                                        id="credential-name"
                                        value={draft.name}
                                        maxLength={256}
                                        placeholder="Provider credential"
                                        aria-invalid={Boolean(errors.name)}
                                        onChange={e => updateDraft({ ...draft, name: e.target.value })}
                                    />
                                    {errors.name ? <FieldError>{errors.name}</FieldError> : null}
                                </Field>
                                <Field orientation="vertical" className="gap-1.5">
                                    <FieldLabel htmlFor="credential-service">Service</FieldLabel>
                                    <Input
                                        id="credential-service"
                                        value={draft.serviceId}
                                        placeholder="openai"
                                        onChange={e => updateDraft({ ...draft, serviceId: e.target.value })}
                                    />
                                    <FieldDescription>
                                        The service the credential calls. Modules use it to find the credential.
                                    </FieldDescription>
                                </Field>
                                <Field orientation="vertical" className="gap-1.5">
                                    <FieldLabel id="credential-type-label" required>
                                        Type
                                    </FieldLabel>
                                    <ToggleGroup
                                        type="single"
                                        className="flex-wrap"
                                        value={draft.strategy}
                                        onValueChange={value => value && changeStrategy(value as CredentialStrategy)}
                                        aria-labelledby="credential-type-label"
                                    >
                                        {CREDENTIAL_STRATEGIES.map(strategy => (
                                            <ToggleGroupItem key={strategy} value={strategy}>
                                                {credentialStrategyLabel(strategy)}
                                            </ToggleGroupItem>
                                        ))}
                                    </ToggleGroup>
                                    <FieldDescription>The type cannot change once the credential exists.</FieldDescription>
                                </Field>
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
                                <ArrowLeftIcon className="size-4" aria-hidden />
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

function StepMarker({ index, label, current, done }: Readonly<{ index: number; label: string; current: boolean; done: boolean }>) {
    const active = current || done;
    return (
        <li className="flex items-center gap-2" aria-current={current ? 'step' : undefined}>
            <span
                className={`flex size-6 items-center justify-center rounded-full text-xs font-medium ${active ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground'}`}
            >
                {done ? '✓' : index}
            </span>
            <span className={current ? 'font-medium' : 'text-muted-foreground'}>{label}</span>
        </li>
    );
}
