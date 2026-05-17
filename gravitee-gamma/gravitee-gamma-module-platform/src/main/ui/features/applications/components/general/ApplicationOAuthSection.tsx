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
import { Button, Card, CardContent, CardDescription, CardHeader, CardTitle, Input, Label, Textarea } from '@gravitee/graphene-core';
import { CopyIcon } from '@gravitee/graphene-core/icons';

import { GrantTypeChips } from './GrantTypeChips';
import type { ApplicationTypeConfig } from '../../types/applicationCreate';
import type { ApplicationGeneralForm } from '../../utils/applicationGeneralMapper';

export interface ApplicationOAuthSectionProps {
    readonly isSimple: boolean;
    readonly form: ApplicationGeneralForm;
    readonly typeConfig: ApplicationTypeConfig | undefined;
    readonly isFormDisabled: boolean;
    readonly onFieldChange: <K extends keyof ApplicationGeneralForm>(key: K, value: ApplicationGeneralForm[K]) => void;
    readonly onCopy: (value: string) => void;
}

export function ApplicationOAuthSection({
    isSimple,
    form,
    typeConfig,
    isFormDisabled,
    onFieldChange,
    onCopy,
}: ApplicationOAuthSectionProps) {
    if (isSimple) {
        return (
            <Card>
                <CardHeader className="pb-3">
                    <CardTitle className="text-sm">OAuth2 Integration</CardTitle>
                    <CardDescription className="text-xs">Client ID used when subscribing to OAuth2 or JWT plans.</CardDescription>
                </CardHeader>
                <CardContent className="max-w-xl space-y-2">
                    <Label htmlFor="simple-client-id">Client ID</Label>
                    <div className="flex gap-2">
                        <Input
                            id="simple-client-id"
                            value={form.simpleClientId}
                            maxLength={300}
                            onChange={e => onFieldChange('simpleClientId', e.target.value)}
                            disabled={isFormDisabled}
                        />
                        <Button
                            type="button"
                            variant="outline"
                            size="icon"
                            onClick={() => onCopy(form.simpleClientId)}
                            disabled={!form.simpleClientId}
                            aria-label="Copy client ID"
                        >
                            <CopyIcon className="size-4" aria-hidden />
                        </Button>
                    </div>
                </CardContent>
            </Card>
        );
    }

    return (
        <div className="space-y-3">
            <h2 className="text-lg font-semibold">OpenID Connect Integration</h2>
            <Card>
                <CardContent className="max-w-2xl space-y-4 pt-6">
                    <div className="space-y-2">
                        <Label>Client ID</Label>
                        <div className="flex gap-2">
                            <Input value={form.oauthClientId} readOnly className="bg-muted/40" />
                            <Button
                                type="button"
                                variant="outline"
                                size="icon"
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
                        <div className="flex gap-2">
                            <Input value={form.oauthClientSecret} readOnly type="password" className="bg-muted/40" />
                            <Button
                                type="button"
                                variant="outline"
                                size="icon"
                                onClick={() => onCopy(form.oauthClientSecret)}
                                disabled={!form.oauthClientSecret}
                                aria-label="Copy client secret"
                            >
                                <CopyIcon className="size-4" aria-hidden />
                            </Button>
                        </div>
                    </div>
                    {typeConfig ? (
                        <div className="space-y-2">
                            <Label>Authorized grant types</Label>
                            <GrantTypeChips
                                typeConfig={typeConfig}
                                options={typeConfig.allowed_grant_types}
                                values={form.grantTypes}
                                onChange={next => onFieldChange('grantTypes', next)}
                                disabled={isFormDisabled}
                            />
                        </div>
                    ) : null}
                    {typeConfig?.requires_redirect_uris ? (
                        <div className="space-y-2">
                            <Label htmlFor="redirect-uris">Redirect URIs</Label>
                            <Textarea
                                id="redirect-uris"
                                value={form.redirectUrisText}
                                onChange={e => onFieldChange('redirectUrisText', e.target.value)}
                                disabled={isFormDisabled}
                                placeholder="One URI per line"
                                rows={3}
                            />
                        </div>
                    ) : null}
                </CardContent>
            </Card>
        </div>
    );
}
