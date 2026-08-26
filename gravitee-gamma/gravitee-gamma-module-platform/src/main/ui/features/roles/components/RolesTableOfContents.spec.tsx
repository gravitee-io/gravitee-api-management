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
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { RolesTableOfContents } from './RolesTableOfContents';
import { roleSectionId } from '../utils/roleSectionId';

const ITEMS = [
    { scope: 'ORGANIZATION', label: 'Organization' },
    { scope: 'ENVIRONMENT', label: 'Environment' },
    { scope: 'API', label: 'API' },
] as const;

class MockIntersectionObserver implements IntersectionObserver {
    static instances: MockIntersectionObserver[] = [];
    readonly root = null;
    readonly rootMargin = '';
    readonly thresholds = [];
    observed: Element[] = [];
    callback: IntersectionObserverCallback;

    constructor(callback: IntersectionObserverCallback) {
        this.callback = callback;
        MockIntersectionObserver.instances.push(this);
    }

    observe(element: Element) {
        this.observed.push(element);
    }
    unobserve() {}
    disconnect() {}
    takeRecords(): IntersectionObserverEntry[] {
        return [];
    }

    fire(entries: Array<{ target: Element; isIntersecting: boolean }>) {
        this.callback(entries as IntersectionObserverEntry[], this);
    }
}

function renderWithSections() {
    MockIntersectionObserver.instances = [];
    return render(
        <div>
            {ITEMS.map(item => (
                <div key={item.scope} id={roleSectionId(item.scope)} />
            ))}
            <RolesTableOfContents items={ITEMS} />
        </div>,
    );
}

describe('RolesTableOfContents', () => {
    beforeEach(() => {
        Element.prototype.scrollIntoView = jest.fn();
        (global as unknown as { IntersectionObserver: typeof IntersectionObserver }).IntersectionObserver =
            MockIntersectionObserver as unknown as typeof IntersectionObserver;
    });

    it('renders a link for every scope, with the first one active by default', () => {
        renderWithSections();

        expect(screen.getByRole('link', { name: 'Organization' })).toHaveAttribute('aria-current', 'location');
        expect(screen.getByRole('link', { name: 'Environment' })).not.toHaveAttribute('aria-current');
        expect(screen.getByRole('link', { name: 'API' })).not.toHaveAttribute('aria-current');
    });

    it('clicking a link scrolls its section into view and marks it active', async () => {
        const user = userEvent.setup();
        renderWithSections();

        await user.click(screen.getByRole('link', { name: 'API' }));

        expect(document.getElementById(roleSectionId('API'))?.scrollIntoView).toHaveBeenCalledWith(
            expect.objectContaining({ behavior: 'smooth' }),
        );
        expect(screen.getByRole('link', { name: 'API' })).toHaveAttribute('aria-current', 'location');
        expect(screen.getByRole('link', { name: 'Organization' })).not.toHaveAttribute('aria-current');
    });

    it('marks the section currently intersecting the viewport as active while scrolling', () => {
        renderWithSections();

        const observer = MockIntersectionObserver.instances[0];
        const environmentSection = document.getElementById(roleSectionId('ENVIRONMENT'))!;
        act(() => {
            observer.fire([{ target: environmentSection, isIntersecting: true }]);
        });

        expect(screen.getByRole('link', { name: 'Environment' })).toHaveAttribute('aria-current', 'location');
        expect(screen.getByRole('link', { name: 'Organization' })).not.toHaveAttribute('aria-current');
    });
});
