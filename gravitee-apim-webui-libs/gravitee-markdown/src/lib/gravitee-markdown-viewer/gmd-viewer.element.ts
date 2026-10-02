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
import { importProvidersFrom, provideZonelessChangeDetection } from '@angular/core';
import { createCustomElement } from '@angular/elements';
import { createApplication } from '@angular/platform-browser';

import { GraviteeMarkdownViewerComponent } from './gravitee-markdown-viewer.component';
import { GraviteeMarkdownViewerModule } from './gravitee-markdown-viewer.module';

export const GMD_VIEWER_ELEMENT_TAG = 'gmd-viewer';

let registration: Promise<void> | undefined;

/**
 * Registers the Gravitee Markdown viewer as the `<gmd-viewer>` custom element, so hosts that are
 * not Angular applications can render Gravitee Markdown. Pass the markdown through the element's
 * `content` property rather than an attribute.
 *
 * Safe to call repeatedly: the registration happens once per page.
 */
export function registerGmdViewerElement(): Promise<void> {
  return (registration ??= defineGmdViewerElement());
}

async function defineGmdViewerElement(): Promise<void> {
  if (customElements.get(GMD_VIEWER_ELEMENT_TAG)) {
    return;
  }

  const applicationRef = await createApplication({
    providers: [
      provideZonelessChangeDetection(),
      // The viewer is declared by a module rather than standalone, so the module's
      // GraviteeMarkdownRendererService has to reach the application injector.
      importProvidersFrom(GraviteeMarkdownViewerModule),
    ],
  });

  // Another copy of this bundle may have registered the tag while the application was starting;
  // defining it twice throws.
  if (customElements.get(GMD_VIEWER_ELEMENT_TAG)) {
    return;
  }

  customElements.define(
    GMD_VIEWER_ELEMENT_TAG,
    createCustomElement(GraviteeMarkdownViewerComponent, { injector: applicationRef.injector }),
  );
}
