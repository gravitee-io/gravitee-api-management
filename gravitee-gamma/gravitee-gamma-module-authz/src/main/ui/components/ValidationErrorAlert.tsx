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
