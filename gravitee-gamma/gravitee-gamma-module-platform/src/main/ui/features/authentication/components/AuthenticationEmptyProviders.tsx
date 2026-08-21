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

import { Button, DataTableEmptyState } from '@gravitee/graphene-core';
import {
    ArrowRightIcon,
    CircleCheckIcon,
    CircleXIcon,
    FingerprintIcon,
    KeyRoundIcon,
    PlusIcon,
    ShieldIcon,
    type LucideIcon,
} from '@gravitee/graphene-core/icons';

import { FeatureTile } from '../../shared/components';

const WITHOUT = [
    'Users can only sign in with a local console account',
    'GitHub, Google, OpenID Connect, and Gravitee AM stay unused',
    'Group and role membership has to be assigned by hand after every login',
] as const;

const WITH = [
    'People sign in with the identity provider your organization already uses',
    'The same provider can also appear on the developer portal',
    'Group and role mappings can run on first login, or every login',
] as const;

const FEATURE_TILES: { readonly Icon: LucideIcon; readonly title: string; readonly description: string }[] = [
    {
        Icon: FingerprintIcon,
        title: 'Choose a provider',
        description: 'Gravitee.io AM, OpenID Connect, Google, or GitHub.',
    },
    {
        Icon: KeyRoundIcon,
        title: 'Connect it',
        description: 'Client id, secret, and the endpoints that provider exposes.',
    },
    {
        Icon: ShieldIcon,
        title: 'Map access',
        description: 'Optionally map IdP claims onto organization groups and roles.',
    },
];

function ComparisonLine({ label, variant }: { label: string; variant: 'positive' | 'negative' }) {
    return (
        <li className="flex items-center gap-1 text-xs text-muted-foreground">
            {variant === 'positive' ? (
                <CircleCheckIcon className="size-3 shrink-0 text-success" aria-hidden />
            ) : (
                <CircleXIcon className="size-3 shrink-0 text-destructive" aria-hidden />
            )}
            {label}
        </li>
    );
}

export function AuthenticationEmptyProviders({ canCreate = false, onAdd }: Readonly<{ canCreate?: boolean; onAdd?: () => void }>) {
    return (
        <DataTableEmptyState
            variant="first-use"
            icon={<FingerprintIcon className="size-8" aria-hidden />}
            title="No identity providers yet"
            description="An identity provider lets people sign in to the management console and developer portal with an external account instead of a local Gravitee password."
            primaryAction={
                canCreate && onAdd ? (
                    <Button type="button" onClick={onAdd}>
                        <PlusIcon className="size-4" aria-hidden />
                        Add an identity provider
                    </Button>
                ) : undefined
            }
        >
            <div className="flex flex-col items-stretch gap-4 md:flex-row">
                <div className="flex-1 space-y-3 rounded-xl border p-4">
                    <p className="text-xs font-semibold text-muted-foreground">Without a provider</p>
                    <ul className="space-y-1">
                        {WITHOUT.map(label => (
                            <ComparisonLine key={label} label={label} variant="negative" />
                        ))}
                    </ul>
                </div>
                <div className="hidden shrink-0 items-center justify-center md:flex">
                    <ArrowRightIcon className="size-5 text-primary" aria-hidden />
                </div>
                <div className="flex-1 space-y-3 rounded-xl border border-primary/20 bg-primary/5 p-4">
                    <p className="text-xs font-semibold text-primary">With a provider</p>
                    <ul className="space-y-1">
                        {WITH.map(label => (
                            <ComparisonLine key={label} label={label} variant="positive" />
                        ))}
                    </ul>
                </div>
            </div>

            <div className="flex flex-col gap-4 pt-5 md:flex-row">
                {FEATURE_TILES.map(({ Icon, title, description }) => (
                    <div key={title} className="flex-1">
                        <FeatureTile Icon={Icon} title={title} description={description} />
                    </div>
                ))}
            </div>
        </DataTableEmptyState>
    );
}
