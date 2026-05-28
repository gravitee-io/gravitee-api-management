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

export interface AddCertificateSubmit {
    name: string;
    certificate: string;
    endsAt?: string;
    gracePeriodEnd?: string;
    activeCertificateId?: string;
}

export const PEM_PLACEHOLDER = '-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----';

export const INVALID_CERTIFICATE_MESSAGE = 'Invalid certificate format';

/** Console `GIO_DIALOG_WIDTH.MEDIUM` (~600px); step content uses `min-width: 500px` in add-certificate-dialog. */
export const ADD_CERTIFICATE_DIALOG_WIDTH = '37.5rem';
