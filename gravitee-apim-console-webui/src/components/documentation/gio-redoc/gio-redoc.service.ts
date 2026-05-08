/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { DOCUMENT } from '@angular/common';
import { inject, Injectable } from '@angular/core';

export type RedocApi = {
  init: (spec: unknown, options: Record<string, unknown>, element: HTMLElement, callback?: (err?: unknown) => void) => void;
};

type AmdDefine = {
  (moduleName: string, dependencies: string[], factory: () => unknown): void;
  amd?: unknown;
};

type AmdRequire = {
  (dependencies: string[], onLoad: (redocModule: unknown) => void, onError?: (err: unknown) => void): void;
  (moduleName: string): unknown;
};

type RedocWindow = Window & {
  Redoc?: RedocApi;
  define?: unknown;
  require?: unknown;
};

const REDOC_SCRIPT_SRC = 'assets/redoc/redoc.standalone.js';
const REDOC_AMD_MODULE = 'assets/redoc/redoc.standalone';
const REDOC_STANDALONE_AMD_DEPENDENCY = 'null';

@Injectable({ providedIn: 'root' })
export class GioRedocService {
  private readonly document = inject(DOCUMENT);
  private loadPromise: Promise<RedocApi> | null = null;

  load(): Promise<RedocApi> {
    const loadedRedoc = this.getRedocApi();
    if (loadedRedoc) {
      return Promise.resolve(loadedRedoc);
    }

    if (this.loadPromise !== null) {
      return this.loadPromise;
    }

    const loadPromise = this.loadRedoc().catch(err => {
      this.loadPromise = null;
      throw err;
    });
    this.loadPromise = loadPromise;
    return loadPromise;
  }

  private loadRedoc(): Promise<RedocApi> {
    const amdRequire = this.getAmdRequire();
    if (amdRequire) {
      this.ensureRedocStandaloneAmdDependency(amdRequire);
      return this.loadWithAmd(amdRequire);
    }

    return this.loadWithScript();
  }

  private loadWithAmd(amdRequire: AmdRequire): Promise<RedocApi> {
    return new Promise<RedocApi>((resolve, reject) => {
      amdRequire(
        [REDOC_AMD_MODULE],
        redocModule => {
          const redoc = this.extractRedocApi(redocModule);
          if (redoc) {
            resolve(redoc);
            return;
          }
          reject(new Error('Redoc AMD module loaded but Redoc.init is unavailable.'));
        },
        err => reject(err),
      );
    });
  }

  private loadWithScript(): Promise<RedocApi> {
    return new Promise<RedocApi>((resolve, reject) => {
      const script = this.document.createElement('script');
      script.src = REDOC_SCRIPT_SRC;
      script.onload = () => {
        const redoc = this.getRedocApi();
        if (redoc) {
          resolve(redoc);
          return;
        }
        script.remove();
        reject(new Error('Redoc script loaded but window.Redoc.init is unavailable.'));
      };
      script.onerror = err => {
        script.remove();
        reject(err);
      };
      this.document.head.appendChild(script);
    });
  }

  private getRedocApi(): RedocApi | null {
    const redoc = (this.document.defaultView as RedocWindow | null)?.Redoc;
    return typeof redoc?.init === 'function' ? redoc : null;
  }

  private extractRedocApi(redocModule: unknown): RedocApi | null {
    if (this.isRedocApi(redocModule)) {
      return redocModule;
    }

    if (this.isRedocApi((redocModule as { default?: unknown })?.default)) {
      return (redocModule as { default: RedocApi }).default;
    }

    return this.getRedocApi();
  }

  private isRedocApi(value: unknown): value is RedocApi {
    return typeof (value as RedocApi | null)?.init === 'function';
  }

  private ensureRedocStandaloneAmdDependency(amdRequire: AmdRequire): void {
    const amdDefine = this.getAmdDefine();
    if (!amdDefine || this.isAmdModuleAvailable(amdRequire, REDOC_STANDALONE_AMD_DEPENDENCY)) {
      return;
    }

    // Redoc standalone declares this unused AMD dependency.
    amdDefine(REDOC_STANDALONE_AMD_DEPENDENCY, [], () => null);
  }

  private isAmdModuleAvailable(amdRequire: AmdRequire, moduleName: string): boolean {
    try {
      amdRequire(moduleName);
      return true;
    } catch {
      return false;
    }
  }

  private getAmdRequire(): AmdRequire | null {
    const redocWindow = this.document.defaultView as RedocWindow | null;
    return redocWindow && this.hasAmdDefine(redocWindow.define) && typeof redocWindow.require === 'function'
      ? (redocWindow.require as AmdRequire)
      : null;
  }

  private getAmdDefine(): AmdDefine | null {
    const define = (this.document.defaultView as RedocWindow | null)?.define;
    return this.hasAmdDefine(define) ? define : null;
  }

  private hasAmdDefine(define: unknown): define is AmdDefine {
    return typeof define === 'function' && Boolean((define as { amd?: unknown }).amd);
  }
}
