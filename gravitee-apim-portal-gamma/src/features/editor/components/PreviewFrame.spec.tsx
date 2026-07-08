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

import { PreviewFrame } from './PreviewFrame';

describe('PreviewFrame', () => {
    it('should apply viewport data attribute and width for tablet', () => {
        render(
            <PreviewFrame viewport="tablet">
                <div data-testid="child">Portal content</div>
            </PreviewFrame>,
        );

        const frame = screen.getByTestId('child').parentElement;
        expect(frame).toHaveAttribute('data-viewport', 'tablet');
        expect(frame).toHaveStyle({ width: '768px' });
    });

    it('should apply full width for desktop viewport', () => {
        render(
            <PreviewFrame viewport="desktop">
                <div data-testid="child">Portal content</div>
            </PreviewFrame>,
        );

        const frame = screen.getByTestId('child').parentElement;
        expect(frame).toHaveAttribute('data-viewport', 'desktop');
        expect(frame).toHaveStyle({ width: '100%' });
    });

    it('should apply mobile width', () => {
        render(
            <PreviewFrame viewport="mobile">
                <div data-testid="child">Portal content</div>
            </PreviewFrame>,
        );

        const frame = screen.getByTestId('child').parentElement;
        expect(frame).toHaveAttribute('data-viewport', 'mobile');
        expect(frame).toHaveStyle({ width: '390px' });
    });
});
