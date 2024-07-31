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
import { MatCard } from '@angular/material/card';
import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { AsyncPipe } from '@angular/common';

import { DocumentationEditPageComponent } from '../components/documentation-edit-page/documentation-edit-page.component';
import { ApiDocumentationV4Module } from '../api-documentation-v4.module';
import { Api } from '../../../../entities/management-api-v2';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';

@Component({
  selector: 'documentation-edit-homepage',
  standalone: true,
  templateUrl: './documentation-edit-homepage.component.html',
  imports: [DocumentationEditPageComponent, MatCard, ApiDocumentationV4Module, AsyncPipe],
  styleUrl: './documentation-edit-homepage.component.scss',
})
export class DocumentationEditHomepageComponent implements OnInit {
  api$: Observable<Api>;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiV2Service: ApiV2Service,
  ) {}

  ngOnInit() {
    this.api$ = this.apiV2Service.get(this.activatedRoute.snapshot.params.apiId);
  }
}
