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
import { renderWithGraphene } from '@gravitee/graphene-core/testing';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';

import { GraviteeDocsRenderer } from './GraviteeDocsRenderer';

const nbaSpec = readFileSync(
    join(__dirname, '../../../dummy-spec/api-sports-nba-v2.openapi.json'),
    'utf-8',
);

describe('GraviteeDocsRenderer', () => {
    it('should render API title and operations sidebar', () => {
        renderWithGraphene(<GraviteeDocsRenderer specContent={nbaSpec} />);

        expect(screen.getByTestId('gravitee-docs-renderer')).toBeInTheDocument();
        expect(screen.getByText('API-NBA')).toBeInTheDocument();
        expect(screen.getByText('2.2.5')).toBeInTheDocument();
        expect(screen.getByText('List seasons')).toBeInTheDocument();
    });

    it('should update center panel when selecting another operation', async () => {
        const user = userEvent.setup();
        renderWithGraphene(<GraviteeDocsRenderer specContent={nbaSpec} />);

        expect(screen.getByRole('heading', { level: 2, name: 'API status and account information' })).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: /List seasons/i }));

        expect(screen.getByRole('heading', { level: 2, name: 'List seasons' })).toBeInTheDocument();
        expect(screen.getByText('https://v2.nba.api-sports.io/seasons', { selector: 'code' })).toBeInTheDocument();
    });

    it('should show error message for invalid spec', () => {
        renderWithGraphene(<GraviteeDocsRenderer specContent="{" />);

        expect(screen.getByText('Unable to render spec with Gravitee Docs.')).toBeInTheDocument();
    });
});
