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
import type { HttpMethod } from '../openapi-spec-utils';
import styles from './ApiSpecShared.module.scss';

const METHOD_CLASS: Record<HttpMethod, string> = {
    get: styles.methodGet,
    post: styles.methodPost,
    put: styles.methodPut,
    delete: styles.methodDelete,
    patch: styles.methodPatch,
    options: styles.methodOptions,
    head: styles.methodHead,
    trace: styles.methodTrace,
};

interface MethodBadgeProps {
    readonly method: HttpMethod;
}

export function MethodBadge({ method }: MethodBadgeProps) {
    return (
        <span className={`${styles.methodBadge} ${METHOD_CLASS[method]}`}>
            {method.toUpperCase()}
        </span>
    );
}

interface BlockConfigChipProps {
    readonly label: string;
    readonly tag?: string;
    readonly operationId?: string;
}

export function BlockConfigChip({ label, tag, operationId }: BlockConfigChipProps) {
    return (
        <div className={styles.configChip}>
            <span className={styles.configLabel}>{label}</span>
            {tag ? <span className={styles.configMeta}>tag: {tag}</span> : null}
            {operationId ? <span className={styles.configMeta}>operation: {operationId}</span> : null}
        </div>
    );
}

interface EmptyStateProps {
    readonly message: string;
}

export function EmptyState({ message }: EmptyStateProps) {
    return <p className={styles.empty}>{message}</p>;
}

interface LoadingStateProps {
    readonly message?: string;
}

export function LoadingState({ message = 'Loading API spec…' }: LoadingStateProps) {
    return <p className={styles.loading}>{message}</p>;
}
