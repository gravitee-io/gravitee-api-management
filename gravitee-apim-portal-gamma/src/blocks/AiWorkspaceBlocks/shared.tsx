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
import { useCallback, useState } from 'react';

import styles from './AiWorkspaceBlocks.module.scss';

export function isTruthyProp(value: unknown, fallback = true): boolean {
    if (value === undefined || value === null || value === '') {
        return fallback;
    }
    return value === 'true' || value === true;
}

export function maskKey(key: string): string {
    if (key.length <= 12) {
        return key;
    }
    return `${key.slice(0, 8)}${'•'.repeat(8)}${key.slice(-4)}`;
}

export function formatNumber(value: number): string {
    return value.toLocaleString('en-US');
}

export function formatCurrency(value: number, currency = 'USD'): string {
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency,
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    }).format(value);
}

const CopyIcon = () => (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <rect x="9" y="9" width="13" height="13" rx="2" />
        <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
    </svg>
);

const CheckIcon = () => (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <polyline points="20 6 9 17 4 12" />
    </svg>
);

interface CopyButtonProps {
    readonly value: string;
    readonly label?: string;
    readonly className?: string;
}

export function CopyButton({ value, label = 'Copy', className }: CopyButtonProps) {
    const [copied, setCopied] = useState(false);

    const handleCopy = useCallback(() => {
        void navigator.clipboard?.writeText(value).then(() => {
            setCopied(true);
            window.setTimeout(() => setCopied(false), 1600);
        });
    }, [value]);

    return (
        <button
            type="button"
            className={`${styles.iconButton} ${className ?? ''}`}
            onClick={handleCopy}
            aria-label={label}
        >
            {copied ? <CheckIcon /> : <CopyIcon />}
            {copied ? 'Copied' : label}
        </button>
    );
}
