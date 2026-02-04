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
import { Component, effect, inject, input, untracked } from '@angular/core';

import { GMD_FORM_STATE_STORE, GmdFormStateStore } from '../services/gmd-form-state.store';

/**
 * Host component that provides a scoped form state store and manages its lifecycle.
 * Acts as a DI scope boundary for form state management.
 *
 * Usage:
 * ```html
 * <gmd-form-host [content]="markdownContent">
 *   <gmd-viewer [content]="markdownContent" />
 *   <gmd-form-validation-panel />
 * </gmd-form-host>
 * ```
 */
@Component({
  selector: 'gmd-form-host',
  standalone: true,
  template: '<ng-content />',
  styleUrl: './gmd-form-host.component.scss',
  providers: [
    {
      provide: GMD_FORM_STATE_STORE,
      useFactory: () => new GmdFormStateStore(),
    },
  ],
})
export class GmdFormHostComponent {
  private readonly store = inject(GMD_FORM_STATE_STORE);

  /**
   * Markdown content input.
   * When content changes, the store is reset to clear all field states.
   */
  content = input<string>('');

  // Effect: Reset store when content changes
  private readonly contentSync = effect(() => {
    this.content(); // Track content changes
    untracked(() => {
      this.store.reset();
    });
  });
}
