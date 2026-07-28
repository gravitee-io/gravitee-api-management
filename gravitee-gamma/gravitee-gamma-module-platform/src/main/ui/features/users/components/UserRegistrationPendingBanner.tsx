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
import { Alert, AlertDescription, AlertTitle, Button } from '@gravitee/graphene-core';
import { CheckIcon, TriangleAlertIcon, XIcon } from '@gravitee/graphene-core/icons';

interface UserRegistrationPendingBannerProps {
    readonly onAccept: () => void;
    readonly onReject: () => void;
    readonly isPending: boolean;
}

export function UserRegistrationPendingBanner({ onAccept, onReject, isPending }: UserRegistrationPendingBannerProps) {
    return (
        <Alert variant="default" className="border-warning/40 bg-warning/10">
            <TriangleAlertIcon className="size-4 text-warning" aria-hidden />
            <AlertTitle className="text-warning-foreground">Registration Pending</AlertTitle>
            <AlertDescription className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <span>This user has been pre-registered and is waiting for approval.</span>
                <div className="flex shrink-0 gap-2">
                    <Button
                        type="button"
                        size="sm"
                        variant="outline"
                        disabled={isPending}
                        aria-label="Reject user registration"
                        onClick={onReject}
                    >
                        <XIcon className="size-4" aria-hidden />
                        Reject
                    </Button>
                    <Button type="button" size="sm" disabled={isPending} aria-label="Accept user registration" onClick={onAccept}>
                        <CheckIcon className="size-4" aria-hidden />
                        Accept
                    </Button>
                </div>
            </AlertDescription>
        </Alert>
    );
}
