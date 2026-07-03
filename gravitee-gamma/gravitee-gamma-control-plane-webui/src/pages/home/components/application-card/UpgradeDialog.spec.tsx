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
import { render, screen } from '@testing-library/react';

import { APPLICATIONS, REQUEST_ENTERPRISE_LICENSE_URL } from './applications';
import { UpgradeDialog } from './UpgradeDialog';

const ESM_APP = APPLICATIONS.find(a => a.moduleId === 'esm')!;
const ESM_FEATURES = ESM_APP.upgrade!.features;

describe('UpgradeDialog', () => {
    it('should render the module title, description and every feature when open', () => {
        render(<UpgradeDialog app={ESM_APP} open onOpenChange={() => {}} />);

        expect(screen.getByRole('heading', { name: ESM_APP.title })).toBeTruthy();
        expect(screen.getByText(ESM_APP.description)).toBeTruthy();
        ESM_FEATURES.forEach(feature => {
            expect(screen.getByText(feature)).toBeTruthy();
        });
    });

    it('should point the "Request an enterprise license" CTA to the configured URL', () => {
        render(<UpgradeDialog app={ESM_APP} open onOpenChange={() => {}} />);

        const cta = screen.getByRole('link', { name: /request an enterprise license/i });
        expect(cta.getAttribute('href')).toBe(REQUEST_ENTERPRISE_LICENSE_URL);
    });

    it('should not render its content when closed', () => {
        render(<UpgradeDialog app={ESM_APP} open={false} onOpenChange={() => {}} />);

        expect(screen.queryByText(ESM_FEATURES[0])).toBeNull();
    });
});
