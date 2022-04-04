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

import { Constants } from '../entities/Constants';
import { Installation } from '../entities/installation/installation';
import { Scope } from '../entities/alert';
import { AlertStatus } from '../entities/alerts/alertStatus';

@Injectable({
  providedIn: 'root',
})
export class AlertService {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  get(): Observable<Installation> {
    return this.http.get<Installation>(`${this.constants.org.baseURL}/installation`);
  }

  getStatus(referenceType: Scope.ENVIRONMENT): Observable<AlertStatus>;
  getStatus(referenceType: Scope, referenceId: string): Observable<AlertStatus>;
  getStatus(referenceType: Scope, referenceId?: string): Observable<AlertStatus> {
    return this.http.get<AlertStatus>(`${this.getReferenceURL(referenceType, referenceId)}/alerts/status`);
  }

  private getReferenceURL(referenceType: Scope, referenceId: string) {
    switch (referenceType) {
      case Scope.API:
        return `${this.constants.env.baseURL}/apis/${referenceId}`;
      case Scope.APPLICATION:
        return `${this.constants.env.baseURL}/applications/${referenceId}`;
      default:
        return `${this.constants.env.baseURL}/platform`;
    }
  }
}
