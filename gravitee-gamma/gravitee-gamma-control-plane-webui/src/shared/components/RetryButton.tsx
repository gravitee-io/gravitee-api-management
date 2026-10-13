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

import { useMinimumDuration } from '../hooks/useMinimumDuration';

/** A retry that fails can end within a few milliseconds: "Retrying…" stays long enough to be read. */
const RETRYING_SHOWN_FOR_AT_LEAST_MS = 600;

interface RetryButtonProps {
    /** A retry is running. */
    readonly retrying: boolean;
    readonly onRetry: () => void;
    /** Primary by default: the console does not retry by itself, so the user has to act. */
    readonly variant?: 'default' | 'outline';
    readonly label?: string;
}

/**
 * The way forward of a message that offers to retry. It stays focusable while a retry runs, so the focus
 * is not lost, and its minimum width keeps its size when the label changes.
 */
export function RetryButton({ retrying: running, onRetry, variant, label = 'Retry' }: RetryButtonProps) {
    const retrying = useMinimumDuration(running, RETRYING_SHOWN_FOR_AT_LEAST_MS);

    return (
        <Button variant={variant} className="min-w-24" aria-disabled={retrying} onClick={retrying ? undefined : onRetry}>
            {retrying ? 'Retrying…' : label}
        </Button>
    );
}
