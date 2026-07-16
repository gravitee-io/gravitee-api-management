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

import type { ConsumerAuthProvider } from '../types/consumer-auth.types';
import styles from './SsoProviderButtons.module.scss';

const PROVIDERS: ReadonlyArray<{ id: ConsumerAuthProvider; label: string; glyph: string }> = [
    { id: 'google', label: 'Google', glyph: 'G' },
    { id: 'github', label: 'GitHub', glyph: 'GH' },
    { id: 'microsoft', label: 'Microsoft', glyph: 'MS' },
];

interface SsoProviderButtonsProps {
    readonly disabled?: boolean;
    readonly onProviderClick: (provider: ConsumerAuthProvider) => void;
}

export function SsoProviderButtons({ disabled = false, onProviderClick }: SsoProviderButtonsProps) {
    return (
        <div className={styles.ssoList}>
            {PROVIDERS.map(provider => (
                <Button
                    key={provider.id}
                    type="button"
                    variant="outline"
                    className={styles.ssoButton}
                    disabled={disabled}
                    onClick={() => onProviderClick(provider.id)}
                >
                    <span className={styles.ssoIcon} aria-hidden="true">
                        {provider.glyph}
                    </span>
                    Continue with {provider.label}
                </Button>
            ))}
        </div>
    );
}
