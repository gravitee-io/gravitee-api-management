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

import { DictionaryTypeLabel } from './DictionaryTypeLabel';

describe('DictionaryTypeLabel', () => {
    it('renders Manual without status', () => {
        render(<DictionaryTypeLabel type="MANUAL" state="STOPPED" />);
        expect(screen.queryByText('Manual')).not.toBeNull();
        expect(screen.queryByText('Started')).toBeNull();
        expect(screen.queryByText('Stopped')).toBeNull();
    });

    it('renders Dynamic with Started status', () => {
        render(<DictionaryTypeLabel type="DYNAMIC" state="STARTED" />);
        expect(screen.getByText(/Dynamic/)).not.toBeNull();
        expect(screen.queryByText('Started')).not.toBeNull();
    });
});
