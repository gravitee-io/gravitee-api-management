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
import { ApiError } from '../shared/api/authz-api-client';

export interface ValidationErrorAlertProps {
    readonly error: ApiError | Error | null | undefined;
    readonly title?: string;
}

export function ValidationErrorAlert({ error, title = 'Request failed' }: ValidationErrorAlertProps) {
    if (!error) return null;

    const details = error instanceof ApiError ? (error.validation?.errors ?? []) : [];

    return (
        <Alert variant="destructive">
            <AlertTitle>{title}</AlertTitle>
            <AlertDescription>
                <div>{error.message}</div>
                {details.length > 0 && (
                    <ul className="mt-2 pl-5">
                        {details.map(d => (
                            <li key={d}>{d}</li>
                        ))}
                    </ul>
                )}
            </AlertDescription>
        </Alert>
    );
}
