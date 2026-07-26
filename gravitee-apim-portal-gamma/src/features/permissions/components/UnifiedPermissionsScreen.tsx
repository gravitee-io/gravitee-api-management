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
import { Alert, AlertDescription, Tabs, TabsContent, TabsList, TabsTrigger } from '@gravitee/graphene-core';

import { ConsoleAuthoringGrantsTable } from './ConsoleAuthoringGrantsTable';
import { PortalConsumerPermissions } from './PortalConsumerPermissions';
import { useConsoleDocGrants } from '../hooks/useConsoleDocGrants';
import { useScopeCatalog } from '../hooks/useScopeCatalog';

interface UnifiedPermissionsScreenProps {
    readonly className?: string;
}

/**
 * The platform-module variant: one screen, two principal directories. Portal consumers are the
 * tenants and groups model; console users and teams hold authoring roles on the same scope tree.
 */
export function UnifiedPermissionsScreen({ className }: UnifiedPermissionsScreenProps) {
    const { options: scopeOptions, labelFor } = useScopeCatalog();
    const consoleGrants = useConsoleDocGrants();

    return (
        <Tabs defaultValue="consumers" className={`flex min-h-0 flex-col gap-4 ${className ?? ''}`}>
            <TabsList className="shrink-0">
                <TabsTrigger value="consumers">Portal consumers</TabsTrigger>
                <TabsTrigger value="console">Console users &amp; teams</TabsTrigger>
            </TabsList>

            <TabsContent value="consumers" className="mt-0 min-h-0 flex-1 data-[state=inactive]:hidden">
                <PortalConsumerPermissions className="h-full" />
            </TabsContent>

            <TabsContent value="console" className="mt-0 min-h-0 flex-1 space-y-4 overflow-y-auto data-[state=inactive]:hidden">
                <Alert>
                    <AlertDescription>
                        Console identities are separate from portal consumers. These roles decide who writes the
                        documentation, not who can read or subscribe to an asset.
                    </AlertDescription>
                </Alert>

                <ConsoleAuthoringGrantsTable
                    grants={consoleGrants.grants}
                    scopeOptions={scopeOptions}
                    scopeLabelFor={labelFor}
                    onAdd={async input => {
                        await consoleGrants.addGrant(input);
                    }}
                    onRoleChange={consoleGrants.setRole}
                    onRemove={consoleGrants.removeGrant}
                />
            </TabsContent>
        </Tabs>
    );
}
