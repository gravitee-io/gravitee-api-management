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
import interceptorConfig from './app.interceptor.ajs';

describe('interceptorConfig', () => {
  const showError = jest.fn();
  let responseError: (error: unknown) => Promise<unknown>;

  beforeEach(() => {
    showError.mockReset();
    const httpProvider = { defaults: { headers: { common: {} } }, interceptors: [] };
    interceptorConfig(httpProvider as any, {});
    const timeoutInterceptor = httpProvider.interceptors.find(interceptor => interceptor.name === 'interceptorTimeout');
    const $q = { reject: (error: unknown) => Promise.reject(error) };
    const $injector = { get: () => ({ showError }) };
    responseError = timeoutInterceptor($q, $injector).responseError;
  });

  const failWith = async (error: unknown): Promise<void> => {
    await expect(responseError(error)).rejects.toBe(error);
  };

  it('should name the client timeout when the browser stops a request', async () => {
    await failWith({ status: -1, xhrStatus: 'timeout', config: { timeout: 45000 } });

    expect(showError).toHaveBeenCalledWith(
      'The request took too long and was stopped by the browser after 45s. It may still be running on the server.',
    );
  });

  it('should not name a timeout that is not a number', async () => {
    await failWith({ status: -1, xhrStatus: 'timeout', config: { timeout: Promise.resolve() } });

    expect(showError).toHaveBeenCalledWith(
      'The request took too long and was stopped by the browser. It may still be running on the server.',
    );
  });

  it('should still report an unreachable server on a network error', async () => {
    await failWith({ status: -1, xhrStatus: 'error', config: { timeout: 10000 } });

    expect(showError).toHaveBeenCalledWith('Server unreachable');
  });

  it('should stay silent on an aborted request, an HTTP error or a silent call', async () => {
    await failWith({ status: -1, xhrStatus: 'abort', config: {} });
    await failWith({ status: 500, xhrStatus: 'complete', config: {} });
    await failWith({ status: -1, xhrStatus: 'timeout', config: { silentCall: true } });

    expect(showError).not.toHaveBeenCalled();
  });

  it('should report a failed call without a config or in try-it mode', async () => {
    await failWith({ status: -1, xhrStatus: 'error' });
    await failWith({ status: -1, xhrStatus: 'error', config: { tryItMode: true } });

    expect(showError).toHaveBeenCalledTimes(2);
    expect(showError).toHaveBeenCalledWith('Unable to call the remote service.');
  });
});
