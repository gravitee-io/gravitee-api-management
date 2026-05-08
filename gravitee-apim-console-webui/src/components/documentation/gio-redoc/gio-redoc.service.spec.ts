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
import { TestBed } from '@angular/core/testing';

import { GioRedocService, type RedocApi } from './gio-redoc.service';

const REDOC_SCRIPT_SRC = 'assets/redoc/redoc.standalone.js';

type RedocWindow = Window & {
  Redoc?: RedocApi;
  define?: unknown;
  require?: unknown;
};

describe('GioRedocService', () => {
  let service: GioRedocService;
  let redocApi: RedocApi;
  let redocWindow: RedocWindow;
  let originalDefine: unknown;
  let originalRequire: unknown;

  beforeEach(() => {
    redocApi = { init: jest.fn() };
    redocWindow = window as RedocWindow;
    originalDefine = redocWindow.define;
    originalRequire = redocWindow.require;
    delete redocWindow.Redoc;
    redocWindow.define = undefined;
    redocWindow.require = undefined;

    TestBed.configureTestingModule({});
    service = TestBed.inject(GioRedocService);
  });

  afterEach(() => {
    document.head.querySelectorAll(`script[src="${REDOC_SCRIPT_SRC}"]`).forEach(el => el.remove());
    delete redocWindow.Redoc;
    redocWindow.define = originalDefine;
    redocWindow.require = originalRequire;
  });

  it('should append a script element with the correct src to document.head', () => {
    service.load();

    const script = document.head.querySelector(`script[src="${REDOC_SCRIPT_SRC}"]`);
    expect(script).toBeTruthy();
  });

  it('should return the same Promise on repeated calls (singleton)', () => {
    const firstPromise = service.load();
    const secondPromise = service.load();

    expect(firstPromise).toBe(secondPromise);
  });

  it('should append only one script element on repeated calls', () => {
    service.load();
    service.load();

    const scripts = document.head.querySelectorAll(`script[src="${REDOC_SCRIPT_SRC}"]`);
    expect(scripts.length).toBe(1);
  });

  it('should return the already available Redoc API without loading the script again', async () => {
    redocWindow.Redoc = redocApi;

    await expect(service.load()).resolves.toBe(redocApi);

    const scripts = document.head.querySelectorAll(`script[src="${REDOC_SCRIPT_SRC}"]`);
    expect(scripts.length).toBe(0);
  });

  it('should resolve when the script onload fires', async () => {
    const loadPromise = service.load();
    const script = document.head.querySelector(`script[src="${REDOC_SCRIPT_SRC}"]`) as HTMLScriptElement;

    redocWindow.Redoc = redocApi;
    script.onload!(new Event('load'));

    await expect(loadPromise).resolves.toBe(redocApi);
  });

  it('should load Redoc with AMD require when Monaco AMD loader is available', async () => {
    const amdDefine = Object.assign(jest.fn(), { amd: {} });
    const amdRequire = jest.fn((dependencies: string[] | string, onLoad?: (redocModule: unknown) => void) => {
      if (typeof dependencies === 'string') {
        throw new Error(`Module ${dependencies} is not available`);
      }

      expect(dependencies).toEqual(['assets/redoc/redoc.standalone']);
      if (!onLoad) {
        throw new Error('Missing Redoc AMD onLoad callback');
      }
      onLoad(redocApi);
    });
    redocWindow.define = amdDefine;
    redocWindow.require = amdRequire;

    const loadPromise = service.load();

    await expect(loadPromise).resolves.toBe(redocApi);
    expect(amdDefine).toHaveBeenCalledWith('null', [], expect.any(Function));
    expect((amdDefine.mock.calls[0][2] as () => unknown)()).toBeNull();
    expect(amdRequire).toHaveBeenCalledTimes(2);
    expect(amdRequire).toHaveBeenCalledWith('null');
    expect(redocWindow.define).toBe(amdDefine);

    const script = document.head.querySelector(`script[src="${REDOC_SCRIPT_SRC}"]`);
    expect(script).toBeNull();
  });

  it('should not redefine the Redoc standalone AMD dependency when it is already available', async () => {
    const amdDefine = Object.assign(jest.fn(), { amd: {} });
    const amdRequire = jest.fn((dependencies: string[] | string, onLoad?: (redocModule: unknown) => void) => {
      if (typeof dependencies === 'string') {
        return null;
      }

      onLoad?.(redocApi);
      return undefined;
    });
    redocWindow.define = amdDefine;
    redocWindow.require = amdRequire;

    await expect(service.load()).resolves.toBe(redocApi);

    expect(amdDefine).not.toHaveBeenCalled();
    expect(amdRequire).toHaveBeenCalledWith('null');
    expect(amdRequire).toHaveBeenCalledWith(['assets/redoc/redoc.standalone'], expect.any(Function), expect.any(Function));
  });

  it('should reject when the script loads without exposing the Redoc API', async () => {
    const loadPromise = service.load();
    const script = document.head.querySelector(`script[src="${REDOC_SCRIPT_SRC}"]`) as HTMLScriptElement;

    script.onload!(new Event('load'));

    await expect(loadPromise).rejects.toThrow('Redoc script loaded but window.Redoc.init is unavailable.');
  });

  it('should reject when the script onerror fires', async () => {
    const loadPromise = service.load();
    const script = document.head.querySelector(`script[src="${REDOC_SCRIPT_SRC}"]`) as HTMLScriptElement;

    script.onerror!(new ErrorEvent('error'));

    await expect(loadPromise).rejects.toBeDefined();
  });

  it('should allow retry after loading failed', async () => {
    const firstLoadPromise = service.load();
    const firstScript = document.head.querySelector(`script[src="${REDOC_SCRIPT_SRC}"]`) as HTMLScriptElement;

    firstScript.onerror!(new ErrorEvent('error'));

    await expect(firstLoadPromise).rejects.toBeDefined();

    const secondLoadPromise = service.load();
    const secondScript = document.head.querySelector(`script[src="${REDOC_SCRIPT_SRC}"]`) as HTMLScriptElement;

    redocWindow.Redoc = redocApi;
    secondScript.onload!(new Event('load'));

    await expect(secondLoadPromise).resolves.toBe(redocApi);
  });
});
