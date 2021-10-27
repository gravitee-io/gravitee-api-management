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
import { Tag } from '../entities/tag/tag';
import { NewTag } from '../entities/tag/newTag';

@Injectable({
  providedIn: 'root',
})
export class TagService {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  list(): Observable<Tag[]> {
    return this.http.get<Tag[]>(`${this.constants.org.baseURL}/configuration/tags`);
  }

  get(id: string): Observable<Tag> {
    return this.http.get<Tag>(`${this.constants.org.baseURL}/configuration/tags/${id}`);
  }

  create(newTag: NewTag): Observable<Tag> {
    return this.http.post<Tag>(`${this.constants.org.baseURL}/configuration/tags`, newTag);
  }

  update(tag: Tag): Observable<Tag> {
    return this.http.put<Tag>(`${this.constants.org.baseURL}/configuration/tags/${tag.id}`, tag);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.org.baseURL}/configuration/tags/${id}`);
  }
}
