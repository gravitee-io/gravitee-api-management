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
import { Alert, AlertDescription, AlertTitle } from '@gravitee/graphene-core';
import type { ApiError } from '../lib/api/authz-api-client';

export interface ValidationErrorAlertProps {
    readonly error: ApiError | Error | null | undefined;
    readonly title?: string;
}

function isApiError(e: unknown): e is ApiError {
    return e !== null && typeof e === 'object' && 'status' in e && 'validation' in e;
}

export function ValidationErrorAlert({ error, title = 'Request failed' }: ValidationErrorAlertProps) {
    if (!error) return null;

    const message = error.message;
    const details = isApiError(error) ? (error.validation?.errors ?? []) : [];

    return (
        <Alert variant="destructive">
            <AlertTitle>{title}</AlertTitle>
            <AlertDescription>
                <div>{message}</div>
                {details.length > 0 && (
                    <ul style={{ margin: '0.5rem 0 0', paddingLeft: '1.25rem' }}>
                        {details.map((d, i) => (
                            <li key={i}>{d}</li>
                        ))}
                    </ul>
                )}
            </AlertDescription>
        </Alert>
    );
}
