/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { http, HttpResponse } from 'msw';

import { getReCaptchaHeaderName, resetReCaptchaConfigCacheForTests, resolveReCaptchaToken } from './recaptcha.service';
import { useBootstrapStore } from '../../../shared/config/bootstrap.store';
import { buildBootstrapConfig, TEST_MANAGEMENT_BASE } from '../../../testing/factories';
import { server } from '../../../testing/server';

describe('recaptcha.service', () => {
    beforeEach(() => {
        resetReCaptchaConfigCacheForTests();
        useBootstrapStore.setState({
            config: buildBootstrapConfig(),
            loading: false,
            error: null,
        });
    });

    it('returns null when reCAPTCHA is disabled in console config', async () => {
        server.use(http.get(`${TEST_MANAGEMENT_BASE}/console`, () => HttpResponse.json({ reCaptcha: { enabled: false } })));

        await expect(resolveReCaptchaToken('register')).resolves.toBeNull();
    });

    it('exposes the management API header name used by changePassword', () => {
        expect(getReCaptchaHeaderName()).toBe('X-Recaptcha-Token');
    });
});
