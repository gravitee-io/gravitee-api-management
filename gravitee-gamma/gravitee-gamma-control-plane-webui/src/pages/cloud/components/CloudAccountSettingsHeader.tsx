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
import { Badge, Button, toast } from '@gravitee/graphene-core';
import { CopyIcon } from '@gravitee/graphene-core/icons';
import { useCallback } from 'react';

import { CLOUD_ACCOUNT_MOCK } from '../cloud.config';

function CopyAccountIdButton({ accountId }: { readonly accountId: string }) {
    const handleCopy = useCallback(() => {
        void navigator.clipboard.writeText(accountId).then(() => {
            toast.success('Account ID copied to clipboard');
        });
    }, [accountId]);

    return (
        <Button type="button" variant="ghost" size="icon-sm" onClick={handleCopy} aria-label="Copy account ID">
            <CopyIcon className="size-3.5" aria-hidden />
        </Button>
    );
}

export function CloudAccountSettingsHeader({ title }: { readonly title: string }) {
    return (
        <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
            <div className="flex items-center gap-1">
                <Badge variant="secondary" className="font-normal" data-testid="account-id-badge">
                    Account ID: {CLOUD_ACCOUNT_MOCK.id}
                </Badge>
                <CopyAccountIdButton accountId={CLOUD_ACCOUNT_MOCK.id} />
            </div>
        </div>
    );
}
