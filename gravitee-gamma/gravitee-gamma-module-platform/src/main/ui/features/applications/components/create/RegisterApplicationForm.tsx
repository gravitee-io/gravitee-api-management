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
    Alert,
    AlertDescription,
    Button,
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
    Input,
    Label,
    Separator,
    Skeleton,
    Textarea,
} from '@gravitee/graphene-core';
import { Loader2Icon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { ApplicationGroupsField } from './ApplicationGroupsField';
import { ApplicationTypeSelector } from './ApplicationTypeSelector';
import { OAuthSecurityFields } from './OAuthSecurityFields';
import { SimpleSecurityFields } from './SimpleSecurityFields';
import { useApplicationGroups } from '../../hooks/useApplicationGroups';
import { useApplicationTypes } from '../../hooks/useApplicationTypes';
import { useCreateApplication } from '../../hooks/useCreateApplication';
import { useUserGroupRequired } from '../../hooks/useUserGroupRequired';
import type { RegisterApplicationDraft } from '../../types/applicationCreate';
import { defaultGrantTypesForType, isOAuthApplicationType, isRegisterApplicationFormValid } from '../../utils/applicationCreateMapper';

const EMPTY_DRAFT: RegisterApplicationDraft = {
    name: '',
    description: '',
    domain: '',
    typeId: 'simple',
    groups: [],
    appType: '',
    appClientId: '',
    grantTypes: [],
    redirectUris: '',
    clientCertificate: '',
    additionalClientMetadata: null,
};

export function RegisterApplicationForm() {
    const navigate = useNavigate();
    const { data: applicationTypes = [], isLoading: isLoadingTypes } = useApplicationTypes();
    const { data: groups = [], isLoading: isLoadingGroups } = useApplicationGroups();
    const { requireUserGroups } = useUserGroupRequired();
    const createApplication = useCreateApplication();

    const [draft, setDraft] = useState<RegisterApplicationDraft>(EMPTY_DRAFT);

    const selectedType = applicationTypes.find(type => type.id === draft.typeId) ?? applicationTypes[0];

    useEffect(() => {
        if (applicationTypes.length === 0) return;
        if (!applicationTypes.some(type => type.id === draft.typeId)) {
            const firstType = applicationTypes[0];
            setDraft(current => ({
                ...current,
                typeId: firstType.id,
                grantTypes: defaultGrantTypesForType(firstType),
            }));
        }
    }, [draft.typeId, applicationTypes]);

    const updateDraft = (patch: Partial<RegisterApplicationDraft>) => {
        setDraft(current => ({ ...current, ...patch }));
    };

    const handleTypeChange = (typeId: string) => {
        const nextType = applicationTypes.find(type => type.id === typeId);
        if (!nextType) return;
        updateDraft({
            typeId,
            grantTypes: defaultGrantTypesForType(nextType),
            redirectUris: '',
            appType: '',
            appClientId: '',
            additionalClientMetadata: null,
        });
    };

    const isSimpleType = !isOAuthApplicationType(selectedType?.id ?? '');

    const hasGroupsToAdd = groups.length > 0;
    const showGroupsField = hasGroupsToAdd || requireUserGroups;

    const isFormValid = useMemo(
        () => isRegisterApplicationFormValid(draft, selectedType, { requireUserGroups }),
        [draft, selectedType, requireUserGroups],
    );

    const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        if (!selectedType || !isFormValid) {
            return;
        }

        createApplication.mutate(
            { draft, selectedType },
            {
                onSuccess: created =>
                    navigate(`../${created.id}/general`, {
                        state: { created: true, applicationName: created.name },
                    }),
            },
        );
    };

    if (isLoadingTypes && applicationTypes.length === 0) {
        return (
            <div className="space-y-6">
                <Skeleton className="h-10 w-full max-w-xl" />
                <Skeleton className="h-96 w-full" />
            </div>
        );
    }

    return (
        <form className="space-y-6" onSubmit={handleSubmit} noValidate>
            {createApplication.isError && (
                <Alert variant="destructive">
                    <AlertDescription>{createApplication.error.message}</AlertDescription>
                </Alert>
            )}

            <Card>
                <CardHeader>
                    <CardTitle className="text-lg">General</CardTitle>
                    <CardDescription>Provide basic information about the application.</CardDescription>
                </CardHeader>
                <CardContent className="space-y-5">
                    <div className="space-y-2">
                        <Label htmlFor="application-name">
                            Name <span className="text-destructive">*</span>
                        </Label>
                        <Input
                            id="application-name"
                            value={draft.name}
                            onChange={event => updateDraft({ name: event.target.value })}
                            placeholder="Application name"
                            maxLength={512}
                        />
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="application-description">
                            Description <span className="text-destructive">*</span>
                        </Label>
                        <Textarea
                            id="application-description"
                            value={draft.description}
                            onChange={event => updateDraft({ description: event.target.value })}
                            placeholder="Provide a description of your application, what it does, ..."
                            maxLength={40000}
                            rows={4}
                        />
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="application-domain">Domain</Label>
                        <Input
                            id="application-domain"
                            value={draft.domain}
                            onChange={event => updateDraft({ domain: event.target.value })}
                            placeholder="e.g., app.example.com"
                            maxLength={512}
                        />
                    </div>

                    {showGroupsField && (
                        <ApplicationGroupsField
                            groups={groups}
                            selectedGroupIds={draft.groups}
                            onSelectedGroupIdsChange={groupIds => updateDraft({ groups: groupIds })}
                            isLoading={isLoadingGroups}
                            required={requireUserGroups}
                        />
                    )}

                    {requireUserGroups && !hasGroupsToAdd && !isLoadingGroups && (
                        <Alert variant="destructive">
                            <AlertDescription>
                                The current user is not associated to any groups. Add the user to one or more groups or disable the option
                                to make group selection mandatory while creating applications.
                            </AlertDescription>
                        </Alert>
                    )}

                    <Separator />

                    <div className="space-y-4">
                        <h3 className="text-base font-semibold">Security</h3>

                        {applicationTypes.length === 0 && (
                            <Alert>
                                <AlertDescription>
                                    No application type available. Please check Client Registration configuration.
                                </AlertDescription>
                            </Alert>
                        )}

                        {applicationTypes.length > 0 && (
                            <ApplicationTypeSelector
                                types={applicationTypes}
                                selectedTypeId={draft.typeId}
                                onTypeChange={handleTypeChange}
                                readOnly={applicationTypes.length === 1}
                            />
                        )}

                        {!isSimpleType && selectedType && (
                            <OAuthSecurityFields
                                selectedType={selectedType}
                                grantTypes={draft.grantTypes}
                                redirectUris={draft.redirectUris}
                                clientCertificate={draft.clientCertificate}
                                additionalClientMetadata={draft.additionalClientMetadata}
                                onGrantTypesChange={grantTypes => updateDraft({ grantTypes })}
                                onRedirectUrisChange={redirectUris => updateDraft({ redirectUris })}
                                onClientCertificateChange={clientCertificate => updateDraft({ clientCertificate })}
                                onAdditionalClientMetadataChange={additionalClientMetadata => updateDraft({ additionalClientMetadata })}
                            />
                        )}

                        {isSimpleType && (
                            <SimpleSecurityFields
                                appType={draft.appType}
                                appClientId={draft.appClientId}
                                clientCertificate={draft.clientCertificate}
                                onAppTypeChange={appType => updateDraft({ appType })}
                                onAppClientIdChange={appClientId => updateDraft({ appClientId })}
                                onClientCertificateChange={clientCertificate => updateDraft({ clientCertificate })}
                            />
                        )}
                    </div>
                </CardContent>
            </Card>

            <div className="sticky bottom-0 -mx-6 mt-6 border-t bg-background/95 px-6 py-4 backdrop-blur supports-[backdrop-filter]:bg-background/80">
                <div className="flex items-center justify-end gap-3">
                    <Button type="button" variant="outline" onClick={() => navigate('..')}>
                        Cancel
                    </Button>
                    <Button type="submit" disabled={createApplication.isPending || !isFormValid}>
                        {createApplication.isPending && <Loader2Icon className="size-4 animate-spin" aria-hidden />}
                        Create Application
                    </Button>
                </div>
            </div>
        </form>
    );
}
