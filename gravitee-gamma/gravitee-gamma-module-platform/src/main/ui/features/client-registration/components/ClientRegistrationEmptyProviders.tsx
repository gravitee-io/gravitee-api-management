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
import {
    ArrowRightIcon,
    CircleCheckIcon,
    CircleXIcon,
    KeyRoundIcon,
    ServerIcon,
    ShieldIcon,
    type LucideIcon,
} from '@gravitee/graphene-core/icons';

import { FeatureTile } from '../../shared/components';

const WITHOUT_PROVIDER = [
    'OAuth applications must be created by hand on the authorization server',
    'Client IDs and secrets drift from what APIM has stored',
    'Application type does not drive the security settings the IdP applies',
] as const;

const WITH_PROVIDER = [
    'Creating an application registers an OAuth client automatically',
    'The IdP applies the right grant types for Browser, Web, Native, or B2B',
    'One OpenID Connect DCR provider is enough for the environment',
] as const;

const FEATURE_TILES: { readonly Icon: LucideIcon; readonly title: string; readonly description: string }[] = [
    {
        Icon: ServerIcon,
        title: 'Point at the IdP',
        description: 'Use the OpenID Connect discovery endpoint so APIM can read the registration URL.',
    },
    {
        Icon: KeyRoundIcon,
        title: 'Prove who you are',
        description: 'Authenticate with client credentials or an initial access token from the authorization server.',
    },
    {
        Icon: ShieldIcon,
        title: 'Register on create',
        description: 'When someone creates a Browser, Web, Native, or B2B application, APIM registers the OAuth client.',
    },
];

function FlowNode({ Icon, label }: { Icon: LucideIcon; label: string }) {
    return (
        <div className="flex flex-col items-center text-center">
            <div className="rounded-lg bg-muted p-2">
                <Icon className="size-4 text-muted-foreground" aria-hidden />
            </div>
            <p className="mt-1 text-xs font-medium">{label}</p>
        </div>
    );
}

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

export function ClientRegistrationEmptyProviders() {
    return (
        <div className="space-y-6 rounded-xl border p-4">
            <div>
                <h3 className="text-base font-semibold">Why add a DCR provider?</h3>
                <p className="mt-1 text-xs text-muted-foreground">
                    A client registration provider is an authorization server that implements OpenID Connect Dynamic Client Registration.
                    Add one so APIM can create the OAuth client when someone registers a Browser, Web, Native, or Backend-to-Backend
                    application.
                </p>
            </div>

            <div className="flex flex-row items-stretch gap-4">
                <div className="flex-1 space-y-3 rounded-xl border p-4">
                    <p className="text-xs font-semibold text-muted-foreground">Without a provider</p>
                    <div className="flex items-center justify-center gap-2">
                        <FlowNode Icon={KeyRoundIcon} label="Application" />
                        <ArrowRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                        <FlowNode Icon={ServerIcon} label="Manual IdP setup" />
                    </div>
                    <ul className="space-y-1">
                        {WITHOUT_PROVIDER.map(label => (
                            <ComparisonLine key={label} label={label} variant="negative" />
                        ))}
                    </ul>
                </div>

                <div className="flex shrink-0 items-center justify-center">
                    <ArrowRightIcon className="size-5 text-primary" aria-hidden />
                </div>

                <div className="flex-1 space-y-3 rounded-xl border border-primary/20 bg-primary/5 p-4">
                    <p className="text-xs font-semibold text-primary">With a provider</p>
                    <div className="flex items-center justify-center gap-2">
                        <FlowNode Icon={KeyRoundIcon} label="Application" />
                        <ArrowRightIcon className="size-4 shrink-0 text-primary/70" aria-hidden />
                        <FlowNode Icon={ShieldIcon} label="DCR" />
                        <ArrowRightIcon className="size-4 shrink-0 text-primary/70" aria-hidden />
                        <FlowNode Icon={ServerIcon} label="OAuth client" />
                    </div>
                    <ul className="space-y-1">
                        {WITH_PROVIDER.map(label => (
                            <ComparisonLine key={label} label={label} variant="positive" />
                        ))}
                    </ul>
                </div>
            </div>

            <div className="flex flex-row gap-4 border-t pt-5">
                {FEATURE_TILES.map(({ Icon, title, description }) => (
                    <div key={title} className="flex-1">
                        <FeatureTile Icon={Icon} title={title} description={description} />
                    </div>
                ))}
            </div>
        </div>
    );
}
