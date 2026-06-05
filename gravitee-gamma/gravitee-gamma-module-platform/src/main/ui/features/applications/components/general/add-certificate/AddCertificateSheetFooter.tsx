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
import { Button, SheetFooter } from '@gravitee/graphene-core';

import { ADD_CERTIFICATE_TEST_IDS } from './addCertificateTestIds';

export function AddCertificateSheetFooter({
    stepIndex,
    isValidating,
    isSubmitting,
    canContinueConfigure,
    onPrevious,
    onValidateAndContinue,
    onContinueToConfirm,
    onSubmit,
    onCancel,
}: Readonly<{
    stepIndex: number;
    isValidating: boolean;
    isSubmitting: boolean;
    canContinueConfigure: boolean;
    onPrevious: () => void;
    onValidateAndContinue: () => void;
    onContinueToConfirm: () => void;
    onSubmit: () => void;
    onCancel: () => void;
}>) {
    return (
        <SheetFooter className="mx-0 mb-0 flex shrink-0 flex-row items-center gap-2 rounded-none border-t border-border bg-popover p-0 px-6 py-4 sm:justify-between">
            {stepIndex > 0 ? (
                <Button
                    type="button"
                    variant="outline"
                    className="mr-auto"
                    data-testid={ADD_CERTIFICATE_TEST_IDS.previousButton}
                    onClick={onPrevious}
                >
                    Previous
                </Button>
            ) : (
                <span className="mr-auto" />
            )}
            <div className="flex gap-2">
                <Button type="button" variant="outline" data-testid={ADD_CERTIFICATE_TEST_IDS.cancelButton} onClick={onCancel}>
                    Cancel
                </Button>
                {stepIndex === 0 ? (
                    <Button
                        type="button"
                        data-testid={ADD_CERTIFICATE_TEST_IDS.validateButton}
                        onClick={onValidateAndContinue}
                        disabled={isValidating}
                    >
                        {isValidating ? 'Validating...' : 'Continue'}
                    </Button>
                ) : null}
                {stepIndex === 1 ? (
                    <Button
                        type="button"
                        data-testid={ADD_CERTIFICATE_TEST_IDS.continueButton}
                        onClick={onContinueToConfirm}
                        disabled={!canContinueConfigure}
                    >
                        Continue
                    </Button>
                ) : null}
                {stepIndex === 2 ? (
                    <Button type="button" data-testid={ADD_CERTIFICATE_TEST_IDS.submitButton} onClick={onSubmit} disabled={isSubmitting}>
                        {isSubmitting ? 'Adding…' : 'Add Certificate'}
                    </Button>
                ) : null}
            </div>
        </SheetFooter>
    );
}
