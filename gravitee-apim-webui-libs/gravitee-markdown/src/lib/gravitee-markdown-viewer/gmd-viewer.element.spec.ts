/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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

import { GMD_VIEWER_ELEMENT_TAG, registerGmdViewerElement } from './gmd-viewer.element';

// These tests log NG0914 ("using zoneless change detection, but is still loading Zone.js"): the
// library's Jest setup loads Zone.js for every other suite, while this element is deliberately
// zoneless. The shipped bundle is built with no polyfills, so the warning is specific to Jest.

type GmdViewerElement = HTMLElement & { content?: string };

async function waitFor<T>(label: string, probe: () => T | undefined | null, timeoutMs = 2000): Promise<T> {
  const deadline = Date.now() + timeoutMs;
  for (;;) {
    const value = probe();
    if (value) {
      return value;
    }
    if (Date.now() > deadline) {
      throw new Error(`Timed out waiting for ${label}`);
    }
    await new Promise(resolve => setTimeout(resolve, 10));
  }
}

describe('registerGmdViewerElement', () => {
  it('should define the gmd-viewer custom element', async () => {
    await registerGmdViewerElement();

    expect(customElements.get(GMD_VIEWER_ELEMENT_TAG)).toBeDefined();
  });

  it('should not fail when called twice', async () => {
    await registerGmdViewerElement();

    await expect(registerGmdViewerElement()).resolves.toBeUndefined();
  });

  it('should not fail when called concurrently', async () => {
    await expect(Promise.all([registerGmdViewerElement(), registerGmdViewerElement()])).resolves.toBeDefined();
  });

  it('should not fail when a second copy of the module registers the same tag', async () => {
    await registerGmdViewerElement();

    // The Gamma host is module-federated, so two copies of this bundle can be loaded.
    jest.resetModules();
    const secondCopy = await import('./gmd-viewer.element');

    await expect(secondCopy.registerGmdViewerElement()).resolves.toBeUndefined();
  });

  it('should render the markdown set on the content property', async () => {
    await registerGmdViewerElement();

    const element: GmdViewerElement = document.createElement(GMD_VIEWER_ELEMENT_TAG);
    document.body.appendChild(element);
    element.content = '# Hello, World!';

    const heading = await waitFor('the heading to render', () => element.shadowRoot?.querySelector('h1'));
    expect(heading?.textContent).toBe('Hello, World!');

    element.remove();
  });
});
