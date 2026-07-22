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
import { Button, Card, CardContent } from '@gravitee/graphene-core';
import {
    ArrowRightIcon,
    CableIcon,
    NetworkIcon,
    XIcon,
    type LucideIcon,
} from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import type { PortalIdentityProviderType } from '../types';

interface OnboardingCardProps {
    readonly Icon: LucideIcon;
    readonly title: string;
    readonly description: string;
    readonly defaultType: PortalIdentityProviderType;
    readonly onGetStarted: (defaultType: PortalIdentityProviderType) => void;
}

function OnboardingCard({ Icon, title, description, defaultType, onGetStarted }: OnboardingCardProps) {
    return (
        <Card className="bg-background">
            <CardContent className="flex h-full flex-col gap-3 pt-5 pb-5">
                <div className="w-fit rounded-lg bg-primary/10 p-2">
                    <Icon className="size-5 text-primary" aria-hidden />
                </div>
                <div className="flex-1">
                    <h3 className="text-sm font-semibold">{title}</h3>
                    <p className="mt-1 text-xs leading-relaxed text-muted-foreground">{description}</p>
                </div>
                <button
                    type="button"
                    className="mt-auto flex items-center gap-1 text-xs font-medium text-primary hover:underline"
                    onClick={() => onGetStarted(defaultType)}
                >
                    Get started
                    <ArrowRightIcon className="size-3" aria-hidden />
                </button>
            </CardContent>
        </Card>
    );
}

const ONBOARDING_CARDS: readonly Omit<OnboardingCardProps, 'onGetStarted'>[] = [
    {
        Icon: CableIcon,
        title: 'Configure Single Sign-On',
        description:
            'Connect your enterprise Identity provider to provide a secure, seamless sign-in experience for developers while centralizing authentication management.',
        defaultType: 'GRAVITEEIO_AM',
    },
    {
        Icon: NetworkIcon,
        title: 'Social Login',
        description:
            'Speed up developer onboarding by enabling sign-in with popular Identity providers, reducing the need for separate portal credentials.',
        defaultType: 'GOOGLE',
    },
];

interface IdentityProvidersOnboardingBannerProps {
    readonly onGetStarted: (defaultType: PortalIdentityProviderType) => void;
}

export function IdentityProvidersOnboardingBanner({ onGetStarted }: IdentityProvidersOnboardingBannerProps) {
    const [dismissed, setDismissed] = useState(false);

    if (dismissed) {
        return null;
    }

    return (
        <section aria-labelledby="idp-onboarding-heading" className="relative space-y-4 rounded-xl bg-muted p-6">
            <div className="flex items-start justify-between gap-4">
                <h2 id="idp-onboarding-heading" className="text-base font-semibold tracking-tight text-foreground">
                    Configure Single Sign-On
                </h2>
                <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="size-8 shrink-0"
                    aria-label="Dismiss onboarding section"
                    onClick={() => setDismissed(true)}
                >
                    <XIcon className="size-4" aria-hidden />
                </Button>
            </div>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                {ONBOARDING_CARDS.map(card => (
                    <OnboardingCard key={card.title} {...card} onGetStarted={onGetStarted} />
                ))}
            </div>
        </section>
    );
}
