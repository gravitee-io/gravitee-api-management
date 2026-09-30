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
import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { ConfigurationService } from './configuration.service';

const flushPromises = () => new Promise(resolve => setTimeout(resolve));

describe('ConfigurationService', () => {
  let spectator: SpectatorHttp<ConfigurationService>;
  const createService = createHttpFactory(ConfigurationService);

  beforeEach(() => {
    spectator = createService();
  });

  it('should be created', () => {
    expect(spectator.service).toBeTruthy();
  });

  describe('load', () => {
    let errorElement: HTMLElement;
    let loaderElement: HTMLElement;

    beforeEach(() => {
      document.body.innerHTML = `
        <span id="loader" class="loader"></span>
        <div id="gravitee-bootstrap-error" style="display: none">
          <span id="gravitee-bootstrap-error-message">Portal API unreachable</span>
          <button id="gravitee-bootstrap-error-retry" type="button">Retry</button>
        </div>
      `;
      errorElement = document.getElementById('gravitee-bootstrap-error');
      loaderElement = document.getElementById('loader');
    });

    afterEach(() => {
      document.body.innerHTML = '';
    });

    it('should show the bootstrap error and flag the failure when the bootstrap call fails', async () => {
      const loaded = spectator.service.load();

      spectator.expectOne('./assets/config.json', HttpMethod.GET).flush({ baseURL: 'https://apim.example.com/portal' });
      spectator
        .expectOne('https://apim.example.com/portal/ui/bootstrap', HttpMethod.GET)
        .flush(null, { status: 400, statusText: 'Bad Request' });

      await expect(loaded).resolves.toEqual(false);
      expect(spectator.service.hasBootstrapFailed()).toEqual(true);
      expect(errorElement.style.display).not.toEqual('none');
      expect(loaderElement.style.display).toEqual('none');
    });

    it('should show the maintenance message when the bootstrap call fails because of maintenance mode', async () => {
      const loaded = spectator.service.load();

      spectator.expectOne('./assets/config.json', HttpMethod.GET).flush({ baseURL: 'https://apim.example.com/portal' });
      spectator
        .expectOne('https://apim.example.com/portal/ui/bootstrap', HttpMethod.GET)
        .flush(
          { errors: [{ code: 'errors.maintenance.mode', message: 'Portal is under maintenance', status: '503' }] },
          { status: 503, statusText: 'Service Unavailable' },
        );

      await expect(loaded).resolves.toEqual(false);
      expect(errorElement.style.display).not.toEqual('none');
      expect(document.getElementById('gravitee-bootstrap-error-message').textContent).toEqual('Portal is under maintenance');
    });

    it('should reload the page when retrying after a bootstrap failure', async () => {
      const reload = jest.fn();
      // The real DOM holds the error elements; only the page reload goes through the fake location
      Object.assign(spectator.service, {
        document: { getElementById: (id: string) => document.getElementById(id), location: { reload } },
      });

      const loaded = spectator.service.load();
      spectator.expectOne('./assets/config.json', HttpMethod.GET).flush({ baseURL: 'https://apim.example.com/portal' });
      spectator
        .expectOne('https://apim.example.com/portal/ui/bootstrap', HttpMethod.GET)
        .flush(null, { status: 400, statusText: 'Bad Request' });
      await loaded;

      document.getElementById('gravitee-bootstrap-error-retry').click();

      expect(reload).toHaveBeenCalledTimes(1);
    });

    it('should keep the bootstrap error hidden when the bootstrap call succeeds', async () => {
      const loaded = spectator.service.load();

      spectator.expectOne('./assets/config.json', HttpMethod.GET).flush({ baseURL: 'https://apim.example.com/portal' });
      spectator
        .expectOne('https://apim.example.com/portal/ui/bootstrap', HttpMethod.GET)
        .flush({ baseURL: 'https://apim.example.com/portal', environmentId: 'DEFAULT' });
      await flushPromises();
      spectator.controller.expectOne('https://apim.example.com/portal/environments/DEFAULT/theme?type=PORTAL').flush({});
      spectator.controller.expectOne('https://apim.example.com/portal/environments/DEFAULT/configuration').flush({});

      await expect(loaded).resolves.toEqual(true);
      expect(spectator.service.hasBootstrapFailed()).toEqual(false);
      expect(errorElement.style.display).toEqual('none');
      expect(loaderElement.style.display).not.toEqual('none');
    });
  });
});
