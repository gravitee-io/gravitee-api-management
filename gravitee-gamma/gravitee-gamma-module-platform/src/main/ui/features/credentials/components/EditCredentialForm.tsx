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
import { Button, Card, CardContent, PageFocused, PageHeader, Separator } from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { CredentialAuthenticationFields } from './CredentialAuthenticationFields';
import { CredentialExpiryField } from './CredentialExpiryField';
import { CredentialFormSection } from './CredentialFormSection';
import { CredentialNameAndServiceFields } from './CredentialNameAndServiceFields';
import { CredentialReadOnlyField } from './CredentialReadOnlyField';
import { useHasEnvironmentPermission } from '../../../shared/hooks/useEnvironmentPermissions';
import { notify } from '../../../shared/notify';
import { useRotateCredential, useUpdateCredential } from '../hooks/useCredentialMutations';
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
import { credentialFailureMessage } from '../utils/credentialErrors';
import { credentialOwnerLabel } from '../utils/credentialOwner';
import { ENVIRONMENT_SECRET_UPDATE_PERMISSION } from '../utils/credentialPermissions';
import { CREDENTIAL_ROTATION_WINDOW_NOTE } from '../utils/credentialRotation';
import { credentialStrategyLabel } from '../utils/credentialStrategies';

export function EditCredentialForm({ credential }: Readonly<{ credential: Credential }>) {
    const navigate = useNavigate();
    const canUpdateSecrets = useHasEnvironmentPermission([ENVIRONMENT_SECRET_UPDATE_PERMISSION]);
    const updateMutation = useUpdateCredential();
    const rotateMutation = useRotateCredential();
    const [draft, setDraft] = useState<CredentialDraft>(() => draftFromCredential(credential));
    const [errors, setErrors] = useState<CredentialDraftErrors>({});
    const managed = draft.mode === 'value';
    const saving = updateMutation.isPending || rotateMutation.isPending;
    const backToCredential = () => navigate('..', { relative: 'path' });

    function updateDraft(next: CredentialDraft) {
        setErrors(current => withoutEditedFieldErrors(current, draft, next));
        setDraft(next);
    }

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
            notify.error(credentialFailureMessage(error, 'The credential could not be updated.'));
            return;
        }
        const values = canUpdateSecrets ? rotationValues(draft) : undefined;
        if (values) {
            try {
                await rotateMutation.mutateAsync({ id: credential.id, payload: { values } });
            } catch (error) {
                notify.error(credentialFailureMessage(error, 'The details were saved, but the secret could not be replaced.'));
                return;
            }
        }
        notify.success(values ? 'Credential updated and its secret replaced' : 'Credential updated');
        backToCredential();
    }

    return (
        <PageFocused>
            <div className="flex flex-col gap-6">
                <div>
                    <Button type="button" variant="ghost" size="sm" onClick={backToCredential}>
                        <ArrowLeftIcon aria-hidden />
                        Back to credential
                    </Button>
                </div>
                <PageHeader title="Edit credential" description="Rename a credential, replace its secret, or change when it expires." />

                <Card>
                    <CardContent className="space-y-6 pt-6">
                        <CredentialFormSection
                            index={1}
                            title="Credential details"
                            description="How the credential is named and what it is for."
                        >
                            <CredentialNameAndServiceFields draft={draft} errors={errors} onChange={updateDraft} />
                            <CredentialReadOnlyField
                                label="Type"
                                description="The type is fixed once a credential exists. Create a new credential to use a different one."
                            >
                                {credentialStrategyLabel(credential.strategy)}
                            </CredentialReadOnlyField>
                        </CredentialFormSection>

                        <Separator />

                        <CredentialFormSection
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
                        </CredentialFormSection>

                        <Separator />

                        <CredentialFormSection
                            index={3}
                            title="Ownership & expiry"
                            description="Who owns this credential, and when it should be replaced."
                        >
                            <CredentialReadOnlyField
                                label="Owner"
                                description="Only the owner or an administrator can change or delete the credential."
                            >
                                {credentialOwnerLabel(credential) ?? '—'}
                            </CredentialReadOnlyField>
                            <CredentialExpiryField
                                id="credential-expires"
                                value={draft.expiresAt}
                                error={errors.expiresAt}
                                onChange={expiresAt => updateDraft({ ...draft, expiresAt })}
                            />
                        </CredentialFormSection>
                    </CardContent>
                </Card>

                <div className="flex items-center justify-between border-t pt-4">
                    <Button type="button" variant="ghost" onClick={backToCredential}>
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
