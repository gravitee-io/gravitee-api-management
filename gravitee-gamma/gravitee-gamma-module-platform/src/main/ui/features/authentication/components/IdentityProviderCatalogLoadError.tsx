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

import { Alert, AlertDescription, AlertTitle, Button } from '@gravitee/graphene-core';

export function IdentityProviderCatalogLoadError({ onRetry }: Readonly<{ onRetry: () => void }>) {
    return (
        <Alert variant="destructive">
            <AlertTitle>Groups and roles could not be loaded</AlertTitle>
            <AlertDescription className="flex flex-wrap items-center justify-between gap-2">
                <span>Group and role mappings cannot be configured until this data is available.</span>
                <Button type="button" variant="outline" size="sm" onClick={onRetry}>
                    Retry
                </Button>
            </AlertDescription>
        </Alert>
    );
}
