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

export function MtlsSecurityFields() {
    return (
        <Alert>
            <AlertDescription>
                mTLS consumers are identified by their X.509 client certificate. No additional security configuration is required here — use
                the Selection Rule below if you need to differentiate between multiple mTLS plans.
            </AlertDescription>
        </Alert>
    );
}
