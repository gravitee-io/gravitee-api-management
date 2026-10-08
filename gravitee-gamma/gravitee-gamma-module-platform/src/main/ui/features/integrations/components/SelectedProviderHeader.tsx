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

import { Button } from '@gravitee/graphene-core';

import { ProviderMonogram } from './ProviderMonogram';
import type { ProviderCatalogEntry } from '../utils/providerLabels';

export function SelectedProviderHeader({
    provider,
    onChange,
    disabled = false,
}: Readonly<{ provider: ProviderCatalogEntry; onChange: () => void; disabled?: boolean }>) {
    return (
        <div className="flex items-center gap-3 rounded-lg border bg-muted/50 p-3">
            <ProviderMonogram monogram={provider.monogram} />
            <div className="flex-1">
                <p className="text-xs text-muted-foreground">Provider</p>
                <p className="text-sm font-medium">{provider.label}</p>
            </div>
            <Button type="button" variant="outline" size="sm" disabled={disabled} onClick={onChange}>
                Change
            </Button>
        </div>
    );
}
