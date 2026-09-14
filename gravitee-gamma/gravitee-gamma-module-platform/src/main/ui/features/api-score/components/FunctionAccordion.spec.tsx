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
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { FunctionAccordion } from './FunctionAccordion';
import type { ScoringFunction } from '../types/rulesets';

const FN: ScoringFunction = {
    name: 'checkTag.js',
    payload: 'module.exports = {}',
    createdAt: '2026-01-01T00:00:00Z',
    referenceId: 'DEFAULT',
    referenceType: 'ENVIRONMENT',
};

describe('FunctionAccordion', () => {
    it('keys rows by filename and exposes delete plus payload', async () => {
        const user = userEvent.setup();
        const onDelete = jest.fn();
        render(<FunctionAccordion functions={[FN]} onDelete={onDelete} />);

        await user.click(screen.getByRole('button', { name: /checkTag\.js/ }));
        expect(screen.getByText('module.exports = {}')).not.toBeNull();
        await user.click(screen.getByRole('button', { name: 'Delete' }));
        expect(onDelete).toHaveBeenCalledWith(FN);
    });
});
