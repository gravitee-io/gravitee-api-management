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
import { useEffect, useState } from 'react';

import { ApiScoringDiagnosticsTable, ScoringAssetTypeBadge } from './ApiScoringDiagnosticsTable';
import { CollapsibleSection } from '../../../components/CollapsibleSection';
import type { ScoringAsset } from '../../../types/scoring';

export function ApiScoringAssetCard({ asset }: Readonly<{ asset: ScoringAsset }>) {
    const hasFindings = asset.diagnostics.length > 0;
    const [open, setOpen] = useState(hasFindings);

    useEffect(() => {
        setOpen(hasFindings);
    }, [hasFindings]);

    return (
        <CollapsibleSection
            open={open}
            onOpenChange={setOpen}
            title={
                <span className="flex min-w-0 items-center gap-2 text-left">
                    <span className="truncate">{asset.name}</span>
                    <ScoringAssetTypeBadge type={asset.type} />
                </span>
            }
        >
            <ApiScoringDiagnosticsTable diagnostics={asset.diagnostics} ariaLabel={`${asset.name} diagnostics`} />
        </CollapsibleSection>
    );
}
