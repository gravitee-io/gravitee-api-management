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
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {PortalPageContent} from '../entities/portal-navigation/portal-page-content';
import {ConfigService} from "./config.service";

@Injectable({
  providedIn: 'root',
})
export class PortalPageContentService {
  constructor(
    private http: HttpClient,
    private readonly configService: ConfigService,
  ) {}

  public getPageContent(contentId: string): Observable<PortalPageContent> {
    return this.http.get<PortalPageContent>(`${this.configService.baseURL}/portal-page-contents/${contentId}`);
  }
}
