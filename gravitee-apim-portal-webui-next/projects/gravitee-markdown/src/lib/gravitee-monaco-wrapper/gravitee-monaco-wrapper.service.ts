/*
 * Copyright (C) 2023 The Gravitee team (http://gravitee.io)
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
import { inject, Injectable } from '@angular/core';
import Monaco from 'monaco-editor';
import { ReplaySubject } from 'rxjs';

import { GRAVITEE_MONACO_EDITOR_CONFIG, GraviteeMonacoEditorConfig } from './data/gravitee-monaco-editor-config';

declare global {
  interface Window {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    require: any;
    monaco: typeof Monaco;
  }
}

@Injectable({
  providedIn: 'root',
})
export class GraviteeMonacoWrapperService {
  private readonly config: GraviteeMonacoEditorConfig = inject(GRAVITEE_MONACO_EDITOR_CONFIG);
  public loaded$ = new ReplaySubject<{ monaco: typeof Monaco }>(1);
  private loaded = false;

  public loadEditor(): Promise<void> {
    if (this.loaded) {
      // Already loaded, do nothing.
      return Promise.resolve();
    }
    this.loaded = true;

    return new Promise<void>(resolve => {
      // Monaco is already loaded
      if (typeof window.monaco === 'object') {
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
    }).then(() => {
      this.loaded$.next({ monaco: window.monaco });
    });
  }
}
