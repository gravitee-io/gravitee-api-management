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

import { Injectable } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';
import { filter } from 'rxjs/operators';

import { ApiDefinition } from './models/ApiDefinition';

@Injectable({
  providedIn: 'root',
})
export class PolicyStudioService {
  private apiDefinitionSubject = new BehaviorSubject<ApiDefinition>(null);

  private apiDefinitionToSaveSubject = new Subject<ApiDefinition>();

  reset() {
    this.apiDefinitionSubject = new BehaviorSubject<ApiDefinition>(null);
  }

  setApiDefinition(apiDefinition: ApiDefinition) {
    return this.apiDefinitionSubject.next(apiDefinition);
  }

  saveApiDefinition(apiDefinition: ApiDefinition) {
    return this.apiDefinitionToSaveSubject.next(apiDefinition);
  }

  getApiDefinition$() {
    return this.apiDefinitionSubject.asObservable().pipe(filter(value => !!value));
  }

  getApiDefinitionToSave$() {
    return this.apiDefinitionToSaveSubject;
  }
}
