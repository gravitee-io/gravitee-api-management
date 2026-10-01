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
import { InfoIcon, XIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useState } from 'react';

const BANNER_STORAGE_KEY = 'gamma-cloud-info-banner-dismissed';

const BANNER_MESSAGE =
    'Welcome to Gravitee Cloud in Gamma Console. Account, organization, and environment administration mirrors the Cockpit experience.';

export function CloudInfoBanner() {
    const [open, setOpen] = useState(() => {
        try {
            return localStorage.getItem(BANNER_STORAGE_KEY) !== 'true';
        } catch {
            return true;
        }
    });

    const dismiss = useCallback(() => {
        setOpen(false);
        try {
            localStorage.setItem(BANNER_STORAGE_KEY, 'true');
        } catch {
            /* ignore */
        }
    }, []);

    if (!open) return null;

    return (
        <div
            role="status"
            data-testid="cloud-home-banner"
            className="flex items-start justify-between gap-3 rounded-lg border border-primary/30 bg-primary/5 px-4 py-3"
        >
            <div className="flex items-start gap-3">
                <InfoIcon className="mt-0.5 size-4 shrink-0 text-primary" aria-hidden />
                <p className="text-sm text-foreground">{BANNER_MESSAGE}</p>
            </div>
            <Button variant="ghost" size="icon-sm" onClick={dismiss} aria-label="Dismiss banner" data-testid="banner-close-button">
                <XIcon aria-hidden />
            </Button>
        </div>
    );
}
