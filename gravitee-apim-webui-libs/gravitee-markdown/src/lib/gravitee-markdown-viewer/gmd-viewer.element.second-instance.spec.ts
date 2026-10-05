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

// Kept apart from gmd-viewer.element.spec.ts: a custom element cannot be undefined, and Jest gives
// each spec file its own jsdom, so this is the only way both instances start with the tag unregistered.
describe('registerGmdViewerElement from two instances of the bundle', () => {
  it('should not fail when both instances register at the same time', async () => {
    // The Gamma host is module-federated, so two instances of this bundle can be loaded.
    const firstInstance = await import('./gmd-viewer.element');
    jest.resetModules();
    const secondInstance = await import('./gmd-viewer.element');

    await expect(Promise.all([firstInstance.registerGmdViewerElement(), secondInstance.registerGmdViewerElement()])).resolves.toEqual([
      undefined,
      undefined,
    ]);
    expect(customElements.get(firstInstance.GMD_VIEWER_ELEMENT_TAG)).toBeDefined();
  });
});
