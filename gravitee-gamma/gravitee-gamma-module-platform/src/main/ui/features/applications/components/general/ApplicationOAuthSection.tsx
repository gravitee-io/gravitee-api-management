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
import { Alert, AlertDescription, Button, Card, CardContent, CardHeader, CardTitle, Input, Label } from '@gravitee/graphene-core';
import { CopyIcon } from '@gravitee/graphene-core/icons';

import { ChipInput } from '../../../shared/components';
import type { ApplicationTypeConfig } from '../../types/applicationCreate';
import type { ApplicationGeneralForm, ApplicationGeneralValidation } from '../../utils/applicationGeneralMapper';
import { AdditionalClientMetadataField } from '../shared/AdditionalClientMetadataField';
import { GrantTypeChips } from '../shared/GrantTypeChips';

export interface ApplicationOAuthSectionProps {
    readonly isSimple: boolean;
    readonly form: ApplicationGeneralForm;
    readonly typeConfig: ApplicationTypeConfig | undefined;
    readonly validation: ApplicationGeneralValidation;
    readonly metadataFieldKey: number;
    readonly isFormDisabled: boolean;
    readonly onFieldChange: <K extends keyof ApplicationGeneralForm>(key: K, value: ApplicationGeneralForm[K]) => void;
    readonly onMetadataDuplicateKeysChange: (hasDuplicates: boolean) => void;
    readonly onCopy: (value: string) => void;
}

export function ApplicationOAuthSection({
    isSimple,
    form,
    typeConfig,
    validation,
    metadataFieldKey,
    isFormDisabled,
    onFieldChange,
    onMetadataDuplicateKeysChange,
    onCopy,
}: ApplicationOAuthSectionProps) {
    if (isSimple) {
        return (
            <Card>
                <CardHeader className="pb-3">
                    <CardTitle className="text-sm">OAuth2 Integration</CardTitle>
                </CardHeader>
                <CardContent className="space-y-2">
                    <Label htmlFor="simple-client-id">Client ID</Label>
                    <div className="flex w-full gap-2">
                        <Input
                            id="simple-client-id"
                            value={form.simpleClientId}
                            maxLength={300}
                            onChange={e => onFieldChange('simpleClientId', e.target.value)}
                            disabled={isFormDisabled}
                            className="min-w-0 flex-1"
                        />
                        <Button
                            type="button"
                            variant="outline"
                            size="icon"
                            className="shrink-0"
                            onClick={() => onCopy(form.simpleClientId)}
                            disabled={!form.simpleClientId}
                            aria-label="Copy client ID"
                        >
                            <CopyIcon className="size-4" aria-hidden />
                        </Button>
                    </div>
                    <p className="text-xs text-muted-foreground">
                        The <code className="rounded bg-muted px-1 py-0.5 text-xs">client_id</code> of the application. This field is
                        required to subscribe to certain types of API plan (OAuth2, JWT).
                    </p>
                </CardContent>
            </Card>
        );
    }

    if (!typeConfig) {
        return null;
    }

    return (
        <Card>
            <CardHeader className="pb-3">
                <CardTitle className="text-sm">OpenID Connect Integration</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
                <div className="space-y-2">
                    <Label>Client ID</Label>
                    <div className="flex w-full gap-2">
                        <Input value={form.oauthClientId} readOnly className="min-w-0 flex-1 bg-muted/40" />
                        <Button
                            type="button"
                            variant="outline"
                            size="icon"
                            className="shrink-0"
                            onClick={() => onCopy(form.oauthClientId)}
                            disabled={!form.oauthClientId}
                            aria-label="Copy client ID"
                        >
                            <CopyIcon className="size-4" aria-hidden />
                        </Button>
                    </div>
                </div>
                <div className="space-y-2">
                    <Label>Client secret</Label>
                    <div className="flex w-full gap-2">
                        <Input value={form.oauthClientSecret} readOnly type="password" className="min-w-0 flex-1 bg-muted/40" />
                        <Button
                            type="button"
                            variant="outline"
                            size="icon"
                            className="shrink-0"
                            onClick={() => onCopy(form.oauthClientSecret)}
                            disabled={!form.oauthClientSecret}
                            aria-label="Copy client secret"
                        >
                            <CopyIcon className="size-4" aria-hidden />
                        </Button>
                    </div>
                </div>
                <Alert className="border-primary/30 bg-primary/5">
                    <AlertDescription className="text-sm text-foreground">
                        Some of the fields below might not be updated by Authorization Provider. Please refer to the Authorization Provider
                        documentation for more information.
                    </AlertDescription>
                </Alert>
                <div className="space-y-2">
                    <Label>
                        Allowed grant types <span className="text-destructive">*</span>
                    </Label>
                    <GrantTypeChips
                        typeConfig={typeConfig}
                        values={form.grantTypes}
                        onChange={next => onFieldChange('grantTypes', next)}
                        disabled={isFormDisabled}
                    />
                    {validation.grantTypes ? (
                        <p className="text-xs text-destructive">{validation.grantTypes}</p>
                    ) : (
                        <p className="text-xs text-muted-foreground">
                            Grant types allowed for the client. Please set only grant types you need for security reasons.
                        </p>
                    )}
                </div>
                {typeConfig.requires_redirect_uris ? (
                    <div className="space-y-2">
                        <Label htmlFor="redirect-uris">Redirect URIs</Label>
                        <ChipInput
                            id="redirect-uris"
                            values={form.redirectUris}
                            onChange={next => onFieldChange('redirectUris', next)}
                            placeholder="Enter a redirect URI"
                            disabled={isFormDisabled}
                        />
                        <p className="text-xs text-muted-foreground">URIs where the authorization server will send OAuth responses</p>
                    </div>
                ) : null}
                <AdditionalClientMetadataField
                    key={metadataFieldKey}
                    value={form.additionalClientMetadata}
                    onChange={next => onFieldChange('additionalClientMetadata', next)}
                    onDuplicateKeysChange={onMetadataDuplicateKeysChange}
                    error={validation.additionalClientMetadata}
                    disabled={isFormDisabled}
                />
            </CardContent>
        </Card>
    );
}
