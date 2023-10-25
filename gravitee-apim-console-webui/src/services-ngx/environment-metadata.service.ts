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
import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Constants } from '../entities/Constants';
import { Metadata, NewMetadata, UpdateMetadata } from '../entities/metadata/metadata';

@Injectable({
  providedIn: 'root',
})
export class EnvironmentMetadataService {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  listMetadata(): Observable<Metadata[]> {
    return this.http.get<Metadata[]>(`${this.constants.env.baseURL}/configuration/metadata/`);
  }

  createMetadata(metadata: NewMetadata): Observable<Metadata> {
    return this.http.post<Metadata>(`${this.constants.env.baseURL}/configuration/metadata/`, metadata);
  }

  updateMetadata(metadata: UpdateMetadata): Observable<Metadata> {
    return this.http.put<Metadata>(`${this.constants.env.baseURL}/configuration/metadata/`, metadata);
  }

  deleteMetadata(metadataKey: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.baseURL}/configuration/metadata/${metadataKey}`);
  }
}
