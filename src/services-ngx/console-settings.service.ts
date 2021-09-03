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
import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { merge } from 'lodash';

import { Constants } from '../entities/Constants';
import { ConsoleSettings } from '../entities/consoleSettings';

@Injectable({
  providedIn: 'root',
})
export class ConsoleSettingsService {
  static isReadonly(settings: ConsoleSettings, property: string): boolean {
    if (settings && settings.metadata && settings.metadata.readonly) {
      return settings.metadata.readonly.some((key) => key === property);
    }
    return false;
  }

  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  save(consoleSettings: ConsoleSettings) {
    return this.http.post(`${this.constants.org.baseURL}/settings/`, consoleSettings).pipe(
      tap((consoleSettings) => {
        // FIXME : It's not very nice to directly modify a provider like that. We should create a service or find another way to do it.
        // To be seen at the end of the Angular migration
        merge(this.constants.org.settings, consoleSettings);
      }),
    );
  }

  get(): Observable<ConsoleSettings> {
    return this.http.get<ConsoleSettings>(`${this.constants.org.baseURL}/settings/`);
  }
}
