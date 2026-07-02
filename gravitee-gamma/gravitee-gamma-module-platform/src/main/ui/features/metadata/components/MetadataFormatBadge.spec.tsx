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

import { MetadataFormatBadge } from './MetadataFormatBadge';
import type { MetadataFormat } from '../types/metadata';

describe('MetadataFormatBadge', () => {
    it.each<[MetadataFormat, string]>([
        ['STRING', 'String'],
        ['NUMERIC', 'Numeric'],
        ['BOOLEAN', 'Boolean'],
        ['DATE', 'Date'],
        ['MAIL', 'Mail'],
        ['URL', 'URL'],
    ])('renders label "%s" for format %s', (format, expectedLabel) => {
        render(<MetadataFormatBadge format={format} />);
        expect(screen.queryByText(expectedLabel)).not.toBeNull();
    });
});
