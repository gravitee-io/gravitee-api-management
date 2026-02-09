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
import { Injectable, Inject, Optional, signal, computed, WritableSignal } from '@angular/core';
import * as Monaco from 'monaco-editor';

import { GmdMonacoEditorConfig } from '../models/monaco-editor-config';
import { GMD_CONFIG } from '../tokens/gmd-config.token';

declare global {
  interface Window {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    require: any;
    monaco: typeof Monaco;
  }
}

// Export to ensure the global declaration is included in generated typings
export {};

@Injectable({
  providedIn: 'root',
})
export class MonacoEditorService {
  // Computed signals for derived state
  readonly loaded = computed(() => this.loadingState() === 'loaded');
  readonly monaco = computed(() => this.monacoInstance());
  readonly loading = computed(() => this.loadingState() === 'loading');

  // Use signals for reactive state management
  private readonly monacoInstance = signal<typeof Monaco | null>(null);
  private readonly loadingState: WritableSignal<'pre_loading' | 'loading' | 'loaded'> = signal('pre_loading');
  private readonly config: GmdMonacoEditorConfig;

  constructor(@Optional() @Inject(GMD_CONFIG) config?: GmdMonacoEditorConfig) {
    this.config = config || {};
  }

  public loadEditor(): Promise<void> {
    if (this.loaded()) {
      return Promise.resolve();
    }

    if (this.loading()) {
      // Already loading, wait for completion
      return new Promise<void>(resolve => {
        const checkLoaded = () => {
          if (this.loaded()) {
            resolve();
          } else {
            setTimeout(checkLoaded, 100);
          }
        };
        checkLoaded();
      });
    }

    this.loadingState.set('loading');

    return new Promise<void>(resolve => {
      // Monaco is already loaded
      if (typeof window.monaco === 'object') {
        this.monacoInstance.set(window.monaco);
        this.loadingState.set('loaded');
        resolve();
        return;
      }

      const onGotAmdLoader = () => {
        const baseUrl = this.config.baseUrl || (window.location.origin + window.location.pathname).replace(/\/$/, '');
        // Load Monaco
        window.require.config({
          paths: { vs: baseUrl + '/assets/monaco-editor/min/vs' },
        });

        window.require(['vs/editor/editor.main'], () => {
          this.monacoInstance.set(window.monaco);
          this.loadingState.set('loaded');
          resolve();
        });
      };

      // Load AMD loader if necessary
      if (!window.require) {
        const loaderScript = document.createElement('script');
        loaderScript.type = 'text/javascript';
        loaderScript.src = 'assets/monaco-editor/min/vs/loader.js';
        loaderScript.addEventListener('load', onGotAmdLoader);
        document.body.appendChild(loaderScript);
      } else {
        onGotAmdLoader();
      }
    });
  }
}
