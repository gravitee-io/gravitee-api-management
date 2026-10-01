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
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@gravitee/graphene-core';
import { useOutletContext } from 'react-router-dom';

interface CloudSettingsOutletContext {
    readonly activeLabel: string;
}

export function CloudSettingsSectionPage() {
    const { activeLabel } = useOutletContext<CloudSettingsOutletContext>();

    return (
        <div className="max-w-screen-xl space-y-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-bold tracking-tight">{activeLabel}</h1>
                <p className="text-sm text-muted-foreground">Gravitee Cloud account settings — coming soon in Gamma Console.</p>
            </div>

            <Card>
                <CardHeader>
                    <CardTitle className="text-base">{activeLabel}</CardTitle>
                    <CardDescription>
                        This section mirrors the Cockpit Cloud settings experience. Backend integration will be added with the Cloud module.
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <p className="text-sm text-muted-foreground">
                        Configuration and management for <span className="font-medium text-foreground">{activeLabel}</span> will appear here
                        once the Cloud control plane API is connected.
                    </p>
                </CardContent>
            </Card>
        </div>
    );
}
