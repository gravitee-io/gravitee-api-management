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
import { Button, Empty, EmptyContent, EmptyDescription, EmptyHeader, EmptyMedia } from '@gravitee/graphene-core';
import { CircleAlert } from 'lucide-react';

import { ModuleMessageTitle } from './ModuleMessageTitle';

export function ModuleUnavailable({ moduleName, onReload }: { readonly moduleName: string; readonly onReload: () => void }) {
    return (
        <Empty role="alert">
            <EmptyHeader>
                {/* Red, which Graphene keeps for errors: the waiting message stays neutral while the console retries. */}
                <EmptyMedia variant="icon" className="bg-destructive/10 text-destructive">
                    <CircleAlert aria-hidden="true" />
                </EmptyMedia>
                <ModuleMessageTitle>{`${moduleName} isn't available right now`}</ModuleMessageTitle>
                <EmptyDescription>
                    It may still be updating. Reload the page in a minute. If this keeps happening, contact your administrator.
                </EmptyDescription>
            </EmptyHeader>
            <EmptyContent>
                {/* Primary on purpose: the console has stopped retrying, so reloading is now the way forward. */}
                <Button onClick={onReload}>Reload page</Button>
            </EmptyContent>
        </Empty>
    );
}
