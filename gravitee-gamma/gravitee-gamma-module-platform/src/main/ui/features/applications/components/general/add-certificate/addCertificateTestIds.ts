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

/** Aligns with console `add-certificate-dialog` harness selectors. */
export const ADD_CERTIFICATE_TEST_IDS = {
    nameInput: 'certificate-name-input',
    pemInput: 'certificate-pem-input',
    expirationInput: 'certificate-expiration-input',
    gracePeriodInput: 'grace-period-input',
    validateButton: 'certificate-validate-button',
    continueButton: 'certificate-continue-button',
    previousButton: 'certificate-previous-button',
    cancelButton: 'certificate-cancel-button',
    submitButton: 'add-certificate-submit',
    summary: 'certificate-summary',
    summaryName: 'certificate-summary-name-value',
    summaryActiveUntil: 'certificate-summary-active-until-value',
    summaryGracePeriod: 'certificate-summary-grace-period-value',
    validationErrorBanner: 'validation-error-banner',
    validationSuccessBanner: 'validation-success-banner',
} as const;
