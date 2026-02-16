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
import { BehaviorSubject, Observable } from 'rxjs';
import { filter, map, shareReplay, tap } from 'rxjs/operators';

import { Constants, EnvSettings } from '../entities/Constants';

@Injectable({
  providedIn: 'root',
})
export class EnvironmentSettingsService {
  private currentSettings: BehaviorSubject<EnvSettings> = new BehaviorSubject<EnvSettings>(null);

  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private constants: Constants,
  ) {}

  load(): Observable<void> {
    return this.http.get<EnvSettings>(`${this.constants.env.baseURL}/portal`).pipe(
      tap(settings => {
        this.constants.env.settings = settings;
        this.currentSettings.next(settings);
      }),
      map(() => {
        return;
      }),
    );
  }

  get(): Observable<EnvSettings> {
    return this.currentSettings.asObservable().pipe(
      filter(settings => settings !== null),
      shareReplay(1),
    );
  }

  getSnapshot(): EnvSettings {
    return this.currentSettings.getValue();
  }
}
