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
import { Alert, AlertDescription, DateTimePicker, Label } from '@gravitee/graphene-core';

import { ADD_CERTIFICATE_TEST_IDS } from './addCertificateTestIds';
import { AddCertificateValidatedBanner } from './AddCertificateValidatedBanner';

export function AddCertificateConfigureStep({
    endsAt,
    gracePeriodEnd,
    hasActive,
    configureErrors,
    onEndsAtChange,
    onGracePeriodEndChange,
}: Readonly<{
    endsAt: Date | undefined;
    gracePeriodEnd: Date | undefined;
    hasActive: boolean;
    configureErrors: { endsAt?: string; gracePeriodEnd?: string };
    onEndsAtChange: (date: Date | undefined) => void;
    onGracePeriodEndChange: (date: Date | undefined) => void;
}>) {
    return (
        <div className="space-y-4">
            <AddCertificateValidatedBanner />
            <div className="space-y-2">
                <Label htmlFor="cert-ends-at">Active until</Label>
                <div data-testid={ADD_CERTIFICATE_TEST_IDS.expirationInput}>
                    <DateTimePicker
                        id="cert-ends-at"
                        value={endsAt}
                        onChange={onEndsAtChange}
                        aria-label="Active until"
                        className="w-full"
                    />
                </div>
                <p className="text-xs text-muted-foreground">When this certificate should stop being active</p>
                {configureErrors.endsAt ? <p className="text-xs text-destructive">{configureErrors.endsAt}</p> : null}
            </div>
            {hasActive ? (
                <>
                    <Alert>
                        <AlertDescription>Both certificates will remain active during the grace period to avoid downtime.</AlertDescription>
                    </Alert>
                    <div className="space-y-2">
                        <Label htmlFor="cert-grace">Grace Period end for current certificate*</Label>
                        <div data-testid={ADD_CERTIFICATE_TEST_IDS.gracePeriodInput}>
                            <DateTimePicker
                                id="cert-grace"
                                value={gracePeriodEnd}
                                onChange={onGracePeriodEndChange}
                                aria-label="Grace Period end for current certificate"
                                className="w-full"
                            />
                        </div>
                        <p className="text-xs text-muted-foreground">When the currently active certificate should be revoked</p>
                        {configureErrors.gracePeriodEnd ? (
                            <p className="text-xs text-destructive">{configureErrors.gracePeriodEnd}</p>
                        ) : null}
                    </div>
                </>
            ) : null}
        </div>
    );
}
