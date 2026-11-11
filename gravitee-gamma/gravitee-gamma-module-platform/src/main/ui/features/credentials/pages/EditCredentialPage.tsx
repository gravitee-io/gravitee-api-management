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
    Separator,
    Skeleton,
} from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';
import { useState, type ReactNode } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { useHasEnvironmentPermission } from '../../../shared/hooks/useEnvironmentPermissions';
import { notify } from '../../../shared/notify';
import { CredentialAuthenticationFields, CredentialExpiryField } from '../components/CredentialAuthenticationFields';
import { useRotateCredential, useUpdateCredential } from '../hooks/useCredentialMutations';
import { useCredential, useCredentialOwnerNames } from '../hooks/useCredentials';
import type { Credential } from '../types/credential';
import {
    draftFromCredential,
    rotationValues,
    toUpdateCredentialPayload,
    validateCredentialDraft,
    withoutEditedFieldErrors,
    type CredentialDraft,
    type CredentialDraftErrors,
} from '../utils/credentialDraft';
import { ENVIRONMENT_SECRET_UPDATE_PERMISSION } from '../utils/credentialPermissions';
import { CREDENTIAL_ROTATION_WINDOW_NOTE } from '../utils/credentialRotation';
import { credentialStrategyLabel } from '../utils/credentialStrategies';

export function EditCredentialPage() {
    const { credentialId } = useParams<{ credentialId: string }>();
    const { data: credential, isLoading, isError } = useCredential(credentialId);

    if (isLoading) {
        return <Skeleton className="h-40 w-full" />;
    }
    if (isError || !credential) {
        return (
            <div className="flex items-center justify-center p-8">
                <p className="text-sm text-muted-foreground">Failed to load the credential. It may have been deleted.</p>
            </div>
        );
    }
    return <EditCredentialForm key={credential.id} credential={credential} />;
}

function EditCredentialForm({ credential }: Readonly<{ credential: Credential }>) {
    const navigate = useNavigate();
    const ownerNames = useCredentialOwnerNames([credential.ownerId]);
    const canUpdateSecrets = useHasEnvironmentPermission([ENVIRONMENT_SECRET_UPDATE_PERMISSION]);
    const updateMutation = useUpdateCredential();
    const rotateMutation = useRotateCredential();
    const [draft, setDraft] = useState<CredentialDraft>(() => draftFromCredential(credential));
    const [errors, setErrors] = useState<CredentialDraftErrors>({});

    function updateDraft(next: CredentialDraft) {
        setErrors(current => withoutEditedFieldErrors(current, draft, next));
        setDraft(next);
    }
    const managed = draft.mode === 'value';
    const saving = updateMutation.isPending || rotateMutation.isPending;

    async function save() {
        const draftErrors = validateCredentialDraft(draft, {
            secretsRequired: !managed,
            now: new Date(),
            currentExpiresAt: credential.expiresAt,
        });
        setErrors(draftErrors);
        if (Object.keys(draftErrors).length > 0) {
            return;
        }
        try {
            await updateMutation.mutateAsync({ id: credential.id, payload: toUpdateCredentialPayload(draft, credential) });
        } catch (error) {
            notify.error(error, 'The credential could not be updated.');
            return;
        }
        const values = canUpdateSecrets ? rotationValues(draft) : undefined;
        if (values) {
            try {
                await rotateMutation.mutateAsync({ id: credential.id, payload: { values } });
            } catch (error) {
                notify.error(error, 'The details were saved, but the secret could not be replaced.');
                return;
            }
        }
        notify.success(values ? 'Credential updated and its secret replaced' : 'Credential updated');
        navigate('..', { relative: 'path' });
    }

    return (
        <PageFocused>
            <div className="space-y-6">
                <div className="space-y-2">
                    <Button
                        type="button"
                        variant="ghost"
                        className="gap-1.5 px-0 text-muted-foreground"
                        onClick={() => navigate('..', { relative: 'path' })}
                    >
                        <ArrowLeftIcon className="size-4" aria-hidden />
                        Back to credential
                    </Button>
                    <h1 className="text-2xl font-semibold tracking-tight">Edit credential</h1>
                    <p className="text-sm text-muted-foreground">Rename a credential, replace its secret, or change when it expires.</p>
                </div>

                <Card>
                    <CardContent className="space-y-6 pt-6">
                        <Section index={1} title="Credential details" description="How the credential is named and what it is for.">
                            <Field orientation="vertical" className="gap-1.5">
                                <FieldLabel htmlFor="credential-name" required>
                                    Name
                                </FieldLabel>
                                <Input
                                    id="credential-name"
                                    value={draft.name}
                                    maxLength={256}
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
                                    onChange={e => updateDraft({ ...draft, serviceId: e.target.value })}
                                />
                            </Field>
                            <ReadOnly
                                label="Type"
                                description="The type is fixed once a credential exists. Create a new credential to use a different one."
                            >
                                {credentialStrategyLabel(credential.strategy)}
                            </ReadOnly>
                        </Section>

                        <Separator />

                        <Section
                            index={2}
                            title="Secret"
                            description={
                                managed
                                    ? 'Secrets are never shown. Leave a value empty to keep the stored one, or type a new one to replace it.'
                                    : 'The secrets this credential references.'
                            }
                        >
                            <CredentialAuthenticationFields
                                idPrefix="credential"
                                draft={draft}
                                errors={errors}
                                onChange={updateDraft}
                                valuesOptional={managed}
                                secretsDisabledReason={
                                    managed && !canUpdateSecrets
                                        ? 'Replacing a stored value needs the permission to update secrets.'
                                        : undefined
                                }
                            />
                            {managed ? <p className="text-sm text-muted-foreground">{CREDENTIAL_ROTATION_WINDOW_NOTE}</p> : null}
                        </Section>

                        <Separator />

                        <Section
                            index={3}
                            title="Ownership & expiry"
                            description="Who owns this credential, and when it should be replaced."
                        >
                            <ReadOnly label="Owner" description="Only the owner or an administrator can change or delete the credential.">
                                {credential.ownerId ? (ownerNames[credential.ownerId] ?? credential.ownerId) : '—'}
                            </ReadOnly>
                            <CredentialExpiryField
                                id="credential-expires"
                                value={draft.expiresAt}
                                error={errors.expiresAt}
                                onChange={expiresAt => updateDraft({ ...draft, expiresAt })}
                            />
                        </Section>
                    </CardContent>
                </Card>

                <div className="flex items-center justify-between border-t pt-4">
                    <Button type="button" variant="ghost" onClick={() => navigate('..', { relative: 'path' })}>
                        Cancel
                    </Button>
                    <Button type="button" disabled={saving} onClick={() => void save()}>
                        {saving ? 'Saving…' : 'Save changes'}
                    </Button>
                </div>
            </div>
        </PageFocused>
    );
}

function Section({
    index,
    title,
    description,
    children,
}: Readonly<{ index: number; title: string; description: string; children: ReactNode }>) {
    return (
        <section className="flex gap-4">
            <span className="flex size-6 shrink-0 items-center justify-center rounded-full bg-muted text-xs font-medium text-muted-foreground">
                {index}
            </span>
            <div className="flex-1 space-y-4">
                <div className="space-y-1">
                    <h2 className="text-sm font-semibold">{title}</h2>
                    <p className="text-xs text-muted-foreground">{description}</p>
                </div>
                {children}
            </div>
        </section>
    );
}

function ReadOnly({ label, description, children }: Readonly<{ label: string; description: string; children: ReactNode }>) {
    return (
        <div className="space-y-1">
            <p className="text-sm font-medium">{label}</p>
            <p className="text-sm">{children}</p>
            <FieldDescription>{description}</FieldDescription>
        </div>
    );
}
