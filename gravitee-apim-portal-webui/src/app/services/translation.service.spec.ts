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
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { TranslateLoader, TranslateService } from '@ngx-translate/core';
import { Title } from '@angular/platform-browser';
import { addTranslations } from '@gravitee/ui-components/src/lib/i18n';
import { of } from 'rxjs';
import { signal } from '@angular/core';

import { TranslationService } from './translation.service';

jest.mock('@gravitee/ui-components/src/lib/i18n', () => ({
  addTranslations: jest.fn(),
  setLanguage: jest.fn(),
}));

describe('TranslationService', () => {
  let service: SpectatorService<TranslationService>;
  // The compiler turns every entry into a function, which is what the store and use() hold.
  const compiled = { 'gv-pagination': { results: () => 'compiled' } };
  const raw = { 'gv-pagination': { results: '{count} results' } };

  const createService = createServiceFactory({
    service: TranslationService,
    providers: [
      mockProvider(Title),
      mockProvider(TranslateService, {
        addLangs: () => undefined,
        setFallbackLang: () => undefined,
        getBrowserLang: () => 'en',
        currentLang: signal('en'),
        use: () => of(compiled),
        get: () => of('Developer portal'),
      }),
      mockProvider(TranslateLoader, { getTranslation: () => of(raw) }),
    ],
  });

  beforeEach(() => {
    jest.clearAllMocks();
    service = createService();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ngx-translate 18 made use() emit the compiled dictionary. Handing that to ui-components makes it
  // render a function body instead of a label, and throw whenever a key takes parameters.
  it('should hand ui-components plain strings rather than the compiled translations', async () => {
    await service.service.load();

    expect(addTranslations).toHaveBeenCalledWith('en', raw, 'en');
    const [, given] = (addTranslations as jest.Mock).mock.calls[0];
    expect(typeof given['gv-pagination'].results).toEqual('string');
  });
});
