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

import { useApiDetailContext } from '../../context/ApiDetailContext';

export function ApiDocumentationPage() {
    const { permissionsReady } = useApiDetailContext();
    const canRead = useHasPermission({ anyOf: ['api-documentation-r'] });

    if (!permissionsReady) {
        return null;
    }

    if (!canRead) {
        return (
            <div className="space-y-6">
                <h1 className="text-2xl font-semibold tracking-tight">Documentation</h1>
                <p className="text-sm text-muted-foreground">You don&apos;t have permission to view this API&apos;s documentation.</p>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Documentation</h1>
                <p className="text-sm text-muted-foreground">
                    Pages, folders and links that describe this API to the consumers who find it in the developer portal.
                </p>
            </div>
        </div>
    );
}
