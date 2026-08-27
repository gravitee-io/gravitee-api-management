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
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { Card, CardContent } from '@gravitee/graphene-core';
import { Outlet } from 'react-router-dom';

function CreateApiForbidden() {
    return (
        <div className="flex items-center justify-center p-12">
            <Card className="max-w-md">
                <CardContent className="p-6 text-center space-y-2">
                    <p className="text-sm font-medium">You don&apos;t have permission to create APIs</p>
                    <p className="text-sm text-muted-foreground">Ask an administrator for the Create API permission on this environment.</p>
                </CardContent>
            </Card>
        </div>
    );
}

/**
 * Route guard for `apis/new/*` — mirrors the `environment-api-c` check already used to gate
 * the Create button on {@link ApisPage}. Uses `useHasPermission`, the same idiom every other
 * permission check in this module uses, rather than the route-level `PermissionGate` component.
 */
export function CreateApiGate() {
    const canCreate = useHasPermission({ anyOf: ['environment-api-c'] });

    if (!canCreate) {
        return <CreateApiForbidden />;
    }

    return <Outlet />;
}
