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
import { Badge, cn, Input, Label, Textarea, Tooltip, TooltipContent, TooltipTrigger } from '@gravitee/graphene-core';
import { CircleCheckIcon, InfoIcon } from '@gravitee/graphene-core/icons';

import { AdditionalClientMetadataField } from './AdditionalClientMetadataField';
import type { ApplicationTypeConfig } from '../../types/applicationCreate';
import { isMandatoryGrantType } from '../../utils/applicationCreateMapper';

interface OAuthSecurityFieldsProps {
    readonly selectedType: ApplicationTypeConfig;
    readonly grantTypes: string[];
    readonly redirectUris: string;
    readonly clientCertificate: string;
    readonly additionalClientMetadata: Record<string, string> | null;
    readonly onGrantTypesChange: (grantTypes: string[]) => void;
    readonly onRedirectUrisChange: (value: string) => void;
    readonly onClientCertificateChange: (value: string) => void;
    readonly onAdditionalClientMetadataChange: (value: Record<string, string> | null) => void;
}

export function OAuthSecurityFields({
    selectedType,
    grantTypes,
    redirectUris,
    clientCertificate,
    additionalClientMetadata,
    onGrantTypesChange,
    onRedirectUrisChange,
    onClientCertificateChange,
    onAdditionalClientMetadataChange,
}: OAuthSecurityFieldsProps) {
    const requiresRedirectUris = Boolean(selectedType.requires_redirect_uris);

    const toggleGrantType = (grantType: string) => {
        if (isMandatoryGrantType(selectedType, grantType)) return;
        onGrantTypesChange(grantTypes.includes(grantType) ? grantTypes.filter(value => value !== grantType) : [...grantTypes, grantType]);
    };

    return (
        <div className="space-y-5 pt-2">
            <div className="space-y-2">
                <Label className="flex items-center gap-2">
                    Allowed grant types
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <button
                                type="button"
                                className="inline-flex items-center justify-center rounded-full text-muted-foreground/60 transition-colors hover:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                                aria-label="Grant types help"
                            >
                                <InfoIcon className="size-4" aria-hidden />
                            </button>
                        </TooltipTrigger>
                        <TooltipContent>
                            Grant types allowed for the client. Please set only grant types you need for security reasons.
                        </TooltipContent>
                    </Tooltip>
                </Label>
                <div className="flex flex-wrap gap-2">
                    {selectedType.allowed_grant_types.map(grantType => {
                        const isActive = grantTypes.includes(grantType.type);
                        const isMandatory = isMandatoryGrantType(selectedType, grantType.type);

                        return (
                            <button
                                key={grantType.type}
                                type="button"
                                className={cn(
                                    'inline-flex items-center gap-1.5 rounded-lg border px-3 py-2 text-sm transition-colors',
                                    isActive ? 'border-primary bg-primary/10 text-primary' : 'border-border hover:bg-accent/50',
                                    isMandatory ? 'cursor-not-allowed opacity-80' : 'cursor-pointer',
                                )}
                                onClick={() => toggleGrantType(grantType.type)}
                                disabled={isMandatory}
                                aria-pressed={isActive}
                            >
                                {isActive && <CircleCheckIcon className="size-3.5" aria-hidden />}
                                {grantType.name}
                                {isMandatory && (
                                    <Badge variant="secondary" className="ml-1 text-xs">
                                        Mandatory
                                    </Badge>
                                )}
                            </button>
                        );
                    })}
                </div>
                <p className="text-xs text-muted-foreground">
                    Grant types allowed for the client. Please set only grant types you need for security reasons.
                </p>
            </div>

            {requiresRedirectUris && (
                <div className="space-y-2">
                    <Label htmlFor="redirect-uris">
                        Redirect URIs <span className="text-destructive">*</span>
                    </Label>
                    <Input
                        id="redirect-uris"
                        value={redirectUris}
                        onChange={event => onRedirectUrisChange(event.target.value)}
                        placeholder="https://app.example.com/callback"
                    />
                    <p className="text-xs text-muted-foreground">
                        URIs where the authorization server will send OAuth responses. Separate multiple URIs with commas.
                    </p>
                </div>
            )}

            <div className="space-y-2">
                <Label htmlFor="client-cert">Client Certificate (PEM Only)</Label>
                <Textarea
                    id="client-cert"
                    value={clientCertificate}
                    onChange={event => onClientCertificateChange(event.target.value)}
                    placeholder={'-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----'}
                    rows={4}
                    className="min-h-20 font-mono text-xs"
                />
                <p className="text-xs text-muted-foreground">
                    The <code className="rounded bg-muted px-1 py-0.5 text-xs">client_certificate</code> of the application. This field is
                    required to subscribe to certain mTLS plans.
                </p>
            </div>

            <AdditionalClientMetadataField
                key={selectedType.id}
                value={additionalClientMetadata}
                onChange={onAdditionalClientMetadataChange}
            />
        </div>
    );
}
