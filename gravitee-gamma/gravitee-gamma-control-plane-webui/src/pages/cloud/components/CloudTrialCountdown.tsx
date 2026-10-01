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
import { Button } from '@gravitee/graphene-core';
import type { CSSProperties } from 'react';

export type TrialState = 'default' | 'will-expire' | 'expired' | 'error';

interface CloudTrialCountdownProps {
    readonly daysRemaining: number | null;
}

function resolveTrialState(daysRemaining: number | null): TrialState {
    if (daysRemaining === null || Number.isNaN(daysRemaining)) return 'error';
    if (daysRemaining <= 0) return 'expired';
    if (daysRemaining <= 5) return 'will-expire';
    return 'default';
}

const STATE_STYLES: Record<TrialState, { container: string; buttonVariant: 'default' | 'destructive' }> = {
    default: {
        container: 'border-sky-200 bg-sky-50 dark:border-sky-900 dark:bg-sky-950/40',
        buttonVariant: 'default',
    },
    'will-expire': {
        container: 'border-amber-200 bg-amber-50 dark:border-amber-900 dark:bg-amber-950/40',
        buttonVariant: 'destructive',
    },
    expired: {
        container: 'border-destructive/40 bg-destructive/5',
        buttonVariant: 'destructive',
    },
    error: {
        container: 'border-destructive/40 bg-destructive/5',
        buttonVariant: 'destructive',
    },
};

function trialTitle(daysRemaining: number | null, state: TrialState): string {
    if (state === 'error') return 'Error during remaining days computation';
    if (state === 'expired') return 'Your enterprise trial has expired';
    if (daysRemaining === 1) return '1 Day remaining of your enterprise trial';
    return `${daysRemaining} Days remaining of your enterprise trial`;
}

export function CloudTrialCountdown({ daysRemaining }: CloudTrialCountdownProps) {
    const state = resolveTrialState(daysRemaining);
    const styles = STATE_STYLES[state];

    return (
        <div
            data-testid="cloud-home-trial-countdown"
            className={`flex flex-col gap-3 rounded-xl border p-6 sm:flex-row sm:items-center sm:justify-between ${styles.container}`}
        >
            <div className="space-y-1">
                <h3 data-testid="cloud-home-trial-title" className="text-base font-semibold tracking-tight">
                    {trialTitle(daysRemaining, state)}
                </h3>
                <p className="text-sm text-muted-foreground">Get in touch to explore our options.</p>
            </div>
            <Button variant={styles.buttonVariant} asChild>
                <a
                    href="https://gravitee.io/contact-us-cockpit"
                    target="_blank"
                    rel="noopener noreferrer"
                    data-testid="gravitee-cloud-contact-link"
                >
                    Contact Gravitee
                </a>
            </Button>
        </div>
    );
}

export const TRIAL_CHIP_ACTIVE_STYLE: CSSProperties = {
    borderColor: 'var(--primary)',
    color: 'var(--primary)',
    backgroundColor: 'color-mix(in oklab, var(--primary) 10%, transparent)',
};
