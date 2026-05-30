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
import { CheckIcon, CopyIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useState } from 'react';
import { toGaplJson } from '../../../shared/entity-gapl-shape';
import type { EntityInstance } from '../../../shared/entity.types';

export function EntityGaplShapeTab({ entity }: { entity: EntityInstance }) {
    const json = toGaplJson(entity);
    const [copied, setCopied] = useState(false);

    useEffect(() => {
        if (!copied) return;
        const timer = setTimeout(() => setCopied(false), 1500);
        return () => clearTimeout(timer);
    }, [copied]);

    async function copy() {
        try {
            await navigator.clipboard?.writeText(json);
            setCopied(true);
        } catch {
            // clipboard unavailable — the JSON stays visible for manual copy
        }
    }

    return (
        <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between gap-2">
                <p className="text-sm text-muted-foreground">The canonical document the Policy Decision Point evaluates against.</p>
                <Button type="button" variant="outline" size="sm" onClick={copy} className="shrink-0">
                    {copied ? <CheckIcon className="mr-2 size-4" aria-hidden /> : <CopyIcon className="mr-2 size-4" aria-hidden />}
                    {copied ? 'Copied' : 'Copy JSON'}
                </Button>
            </div>
            <pre data-testid="gapl-json" className="overflow-auto rounded-md border bg-muted/40 p-3 font-mono text-xs text-foreground">
                {json}
            </pre>
        </div>
    );
}
