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
import { ApimApiError } from '@gravitee/gamma-ui-shared/api';

import { classifyCreatePortalError, validatePortalName } from './createPortal';

describe('portal creation boundaries', () => {
    it('validates blank Unicode names and the original UTF-16 length', () => {
        expect(validatePortalName('\u200b\u00a0')).toBeDefined();
        expect(validatePortalName('  Partners  ')).toBeUndefined();
        expect(validatePortalName('😀'.repeat(128))).toBeUndefined();
        expect(validatePortalName(` ${'a'.repeat(256)}`)).toBeDefined();
    });

    it('distinguishes technical codes from permission and general errors', () => {
        expect(classifyCreatePortalError(new ApimApiError(409, '', { technicalCode: 'portal.name.exists' }))).toBe('duplicate');
        expect(classifyCreatePortalError(new ApimApiError(403, '', { technicalCode: 'portal.limitReached' }))).toBe('limit');
        expect(classifyCreatePortalError(new ApimApiError(403, '', {}))).toBe('permission');
        expect(classifyCreatePortalError(new ApimApiError(400, '', {}))).toBe('invalid');
        expect(classifyCreatePortalError(new TypeError('network'))).toBe('general');
    });
});
