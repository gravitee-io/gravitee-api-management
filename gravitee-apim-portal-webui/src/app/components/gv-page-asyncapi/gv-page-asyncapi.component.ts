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
import { Component, OnInit } from '@angular/core';

import { Page } from '../../../../projects/portal-webclient-sdk/src/lib';
import { PageService } from '../../services/page.service';

import '@asyncapi/web-component/lib/asyncapi-web-component';

@Component({
  selector: 'app-gv-page-asyncapi',
  templateUrl: './gv-page-asyncapi.component.html',
  styleUrls: ['./gv-page-asyncapi.component.css'],
})
export class GvPageAsyncApiComponent implements OnInit {
  page: Page;

  constructor(private pageService: PageService) {}

  ngOnInit() {
    this.page = this.pageService.getCurrentPage();
  }

  openMedia(link: string) {
    window.open(link, '_blank');
  }
}
