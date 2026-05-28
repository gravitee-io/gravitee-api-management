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
import { ADD_CERTIFICATE_TEST_IDS } from './addCertificateTestIds';
import { AddCertificateValidatedBanner } from './AddCertificateValidatedBanner';
import { formatApplicationDateTime } from '../../../utils/applicationFormatters';

export function AddCertificateConfirmStep({
    name,
    endsAt,
    gracePeriodEnd,
    hasActive,
}: Readonly<{
    name: string;
    endsAt: Date | undefined;
    gracePeriodEnd: Date | undefined;
    hasActive: boolean;
}>) {
    return (
        <div className="space-y-4">
            <AddCertificateValidatedBanner />
            <div className="rounded-lg border border-border p-4" data-testid={ADD_CERTIFICATE_TEST_IDS.summary}>
                <p className="mb-3 text-sm font-semibold">Certificate Summary</p>
                <div className="space-y-0 divide-y divide-border">
                    <div className="flex gap-3 py-3 text-sm">
                        <span className="min-w-[9rem] text-muted-foreground">Certificate Name:</span>
                        <span className="ml-auto text-right font-medium" data-testid={ADD_CERTIFICATE_TEST_IDS.summaryName}>
                            {name.trim()}
                        </span>
                    </div>
                    <div className="flex gap-3 py-3 text-sm">
                        <span className="min-w-[9rem] text-muted-foreground">Active until:</span>
                        <span className="ml-auto text-right font-medium" data-testid={ADD_CERTIFICATE_TEST_IDS.summaryActiveUntil}>
                            {endsAt ? formatApplicationDateTime(endsAt.toISOString()) : '—'}
                        </span>
                    </div>
                    {hasActive && gracePeriodEnd ? (
                        <div className="flex gap-3 py-3 text-sm">
                            <span className="min-w-[9rem] text-muted-foreground">Grace period ends:</span>
                            <span className="ml-auto text-right font-medium" data-testid={ADD_CERTIFICATE_TEST_IDS.summaryGracePeriod}>
                                {formatApplicationDateTime(gracePeriodEnd.toISOString())}
                            </span>
                        </div>
                    ) : null}
                </div>
            </div>
        </div>
    );
}
