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
import { Alert, AlertDescription } from '@gravitee/graphene-core';
import { CircleCheckIcon } from '@gravitee/graphene-core/icons';

import { ADD_CERTIFICATE_TEST_IDS } from './addCertificateTestIds';

export function AddCertificateValidatedBanner() {
    return (
        <Alert className="border-success/30 bg-success/5" data-testid={ADD_CERTIFICATE_TEST_IDS.validationSuccessBanner}>
            <CircleCheckIcon className="size-4 text-success" aria-hidden />
            <AlertDescription className="text-success">Certificate validated successfully</AlertDescription>
        </Alert>
    );
}
