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
import { GlobalErrorHandler } from './global-error-handler';

describe('GlobalErrorHandler', () => {
  let handler: GlobalErrorHandler;
  let consoleErrorSpy: jest.SpyInstance;
  let locationReloadSpy: jest.SpyInstance;
  let sessionStorageGetItemSpy: jest.SpyInstance;
  let sessionStorageSetItemSpy: jest.SpyInstance;
  const originalLocation = window.location;

  beforeEach(() => {
    handler = new GlobalErrorHandler();

    consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});

    // To mock window.location.reload
    delete (window as any).location;
    window.location = { ...originalLocation, reload: jest.fn() };
    locationReloadSpy = jest.spyOn(window.location, 'reload');

    sessionStorageGetItemSpy = jest.spyOn(Storage.prototype, 'getItem');
    sessionStorageSetItemSpy = jest.spyOn(Storage.prototype, 'setItem');
  });

  afterEach(() => {
    jest.restoreAllMocks();
    sessionStorage.clear();
    window.location = originalLocation;
  });

  it('should forward non-chunk-load errors to console.error', () => {
    const error = new Error('A regular error');
    handler.handleError(error);
    expect(consoleErrorSpy).toHaveBeenCalledWith(error);
    expect(locationReloadSpy).not.toHaveBeenCalled();
    expect(sessionStorageSetItemSpy).not.toHaveBeenCalled();
  });

  describe('ChunkLoadError handling', () => {
    const chunkLoadErrorString = 'Loading chunk 123 failed';
    const chunkLoadError = new Error(chunkLoadErrorString);

    it('should reload the page on the first chunk load error (string)', () => {
      sessionStorageGetItemSpy.mockReturnValue(null);
      handler.handleError(chunkLoadErrorString);

      expect(sessionStorageSetItemSpy).toHaveBeenCalledWith('chunkLoadRetries', '1');
      expect(locationReloadSpy).toHaveBeenCalled();
      expect(consoleErrorSpy).not.toHaveBeenCalled();
    });

    it('should reload the page on the first chunk load error (Error object)', () => {
      sessionStorageGetItemSpy.mockReturnValue(null);
      handler.handleError(chunkLoadError);

      expect(sessionStorageSetItemSpy).toHaveBeenCalledWith('chunkLoadRetries', '1');
      expect(locationReloadSpy).toHaveBeenCalled();
      expect(consoleErrorSpy).not.toHaveBeenCalled();
    });

    it('should not reload the page if max retries have been reached', () => {
      sessionStorageGetItemSpy.mockReturnValue('1');
      handler.handleError(chunkLoadError);

      expect(sessionStorageSetItemSpy).not.toHaveBeenCalled();
      expect(locationReloadSpy).not.toHaveBeenCalled();
      expect(consoleErrorSpy).toHaveBeenCalledWith('Chunk loading failed multiple times. Please refresh manually.');
    });

    it('should handle different chunk numbers', () => {
      const differentChunkError = 'Loading chunk 456 failed';
      sessionStorageGetItemSpy.mockReturnValue(null);
      handler.handleError(differentChunkError);

      expect(sessionStorageSetItemSpy).toHaveBeenCalledWith('chunkLoadRetries', '1');
      expect(locationReloadSpy).toHaveBeenCalled();
    });
  });
});
