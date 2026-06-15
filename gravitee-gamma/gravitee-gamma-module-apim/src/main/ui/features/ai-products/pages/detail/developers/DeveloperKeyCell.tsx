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
import { useState } from 'react';

import { useApiKeyList } from '../../../../apis/hooks/useSubscriptionApiKeys';
import type { SubscriptionContext } from '../../../../apis/types/subscription';
import { CopyField } from '../../../components/CopyField';

/** Lazily reveals the API key for one developer's subscription (API-key plans only). */
export function DeveloperKeyCell({ ctx, subscriptionId }: { ctx: SubscriptionContext; subscriptionId: string }) {
    const [reveal, setReveal] = useState(false);
    const { data, isLoading } = useApiKeyList(ctx, subscriptionId, 1, 1);

    if (!reveal) {
        return (
            <Button variant="outline" size="sm" onClick={() => setReveal(true)}>
                Reveal key
            </Button>
        );
    }
    if (isLoading) {
        return <span className="text-xs text-muted-foreground">Loading…</span>;
    }
    const key = data?.data?.[0]?.key;
    if (!key) {
        return <span className="text-xs text-muted-foreground">No API key</span>;
    }
    return (
        <div className="max-w-[16rem]">
            <CopyField value={key} secret mono />
        </div>
    );
}
