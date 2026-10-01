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
import { Button, Card, CardContent, Switch, toast } from '@gravitee/graphene-core';
import { PencilIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useCallback, useState } from 'react';

import { CLOUD_SSO_MOCK } from '../cloud.config';

function OpenIdConnectIcon({ className }: { readonly className?: string }) {
    return (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 90 90" className={className} aria-hidden>
            <g transform="translate(-59,-80)">
                <g transform="matrix(0.35277777,0,0,-0.35277777,-15.526842,222.47817)">
                    <path fill="currentColor" d="m 330.10774,359.51207 v -159.9391 -20.0609 l 32,15.0609 v 180.5721 z" />
                    <path fill="currentColor" d="m 440.93004,306.71227 4.417,-45.864 -61.883,13.464" />
                    <path
                        fill="currentColor"
                        d="m 266.10774,248.01987 c 0,22.674 24.707,41.769 58.383,47.598 v 20.325 c -51.51,-6.226 -90.383,-34.267 -90.383,-67.923 0,-34.869 41.725,-63.709 96,-68.508 v 20.061 c -36.516,4.578 -64,24.528 -64,48.447 m 101.617,67.915 v -20.317 c 13.399,-2.319 25.385,-6.727 34.9511,-12.64 l 22.6269,13.984 c -15.42,9.531 -35.322,16.283 -57.578,18.973"
                    />
                </g>
            </g>
        </svg>
    );
}

export function CloudSsoPage() {
    const [configured, setConfigured] = useState<boolean>(CLOUD_SSO_MOCK.configured);
    const [enabled, setEnabled] = useState<boolean>(CLOUD_SSO_MOCK.enabled);

    const handleConfigure = useCallback(() => {
        toast.info('SSO configuration will be available when the Cloud API is connected.');
    }, []);

    const handleToggleEnabled = useCallback((checked: boolean) => {
        setEnabled(checked);
        toast.success(checked ? 'Single Sign On has been enabled' : 'Single Sign On has been disabled');
    }, []);

    const handleEdit = useCallback(() => {
        toast.info('Edit SSO configuration will be available when the Cloud API is connected.');
    }, []);

    const handleDelete = useCallback(() => {
        setConfigured(false);
        setEnabled(false);
        toast.success('Single Sign On configuration successfully deleted');
    }, []);

    return (
        <div className="max-w-3xl space-y-6">
            <h1 className="text-2xl font-bold tracking-tight">Single Sign On</h1>

            <Card>
                <CardContent className="space-y-4 pt-6">
                    <p className="text-sm text-muted-foreground">
                        Single Sign On (SSO) is an authentication method that enables users to access multiple applications with one set
                        of credentials.
                    </p>
                    <p className="text-sm text-muted-foreground">
                        You can set up your Gravitee Cloud account to trust a third-party Identity Provider (IdP) using Oauth 2.0/ Open ID
                        connect.
                    </p>

                    <div className="flex flex-wrap items-center justify-between gap-4 border-t pt-4">
                        <div className="flex items-center gap-2 font-medium">
                            <OpenIdConnectIcon className="size-5" />
                            <span>OpenID Connect</span>
                        </div>

                        {!configured ? (
                            <Button onClick={handleConfigure} data-testid="configure-sso-button">
                                Configure
                            </Button>
                        ) : (
                            <div className="flex items-center gap-2">
                                <label className="flex items-center gap-2 text-sm">
                                    <Switch checked={enabled} onCheckedChange={handleToggleEnabled} aria-label="Enable SSO" />
                                    Enable SSO
                                </label>
                                <Button type="button" variant="ghost" size="icon-sm" onClick={handleEdit} aria-label="Edit SSO configuration">
                                    <PencilIcon aria-hidden />
                                </Button>
                                <Button
                                    type="button"
                                    variant="ghost"
                                    size="icon-sm"
                                    onClick={handleDelete}
                                    aria-label="Delete SSO configuration"
                                    data-testid="delete-sso-button"
                                >
                                    <Trash2Icon aria-hidden />
                                </Button>
                            </div>
                        )}
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}
