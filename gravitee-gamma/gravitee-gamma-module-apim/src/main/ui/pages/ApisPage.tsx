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
import { Button } from '@gravitee/graphene';

import { PermissionGate, useHasPermission } from '@gravitee/gamma-modules-sdk';

export function ApisPage() {
    const canCreateApi = useHasPermission({ anyOf: ['environment-api-c'] });

    return (
        <div>
            <h1>APIs</h1>
            <p>Manage your APIs here.</p>

            {canCreateApi ? (
                <Button type="button" size="sm">
                    Create API
                </Button>
            ) : null}

            <PermissionGate anyOf={['environment-api-u']}>
                <p className="mt-4 text-muted-foreground">You can update APIs in this environment.</p>
            </PermissionGate>
        </div>
    );
}
