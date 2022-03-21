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
import { toDom } from '@gravitee/ui-components/src/lib/text-format';

@Component({
  selector: 'app-gv-page-asciidoc',
  templateUrl: './gv-page-asciidoc.component.html',
  styleUrls: ['./gv-page-asciidoc.component.css'],
})
export class GvPageAsciiDocComponent implements OnInit {
  page: Page;
  pageContent: string;

  constructor(private pageService: PageService) {}

  ngOnInit() {
    this.page = this.pageService.getCurrentPage();
    const asiidocContainer = document.querySelector('.gv-page-container');

    if (this.page && this.page.content && asiidocContainer) {
      toDom(this.page.content).then(({ element }) => {
        element.style.maxWidth = 'inherit';
        this.pageContent = element.innerHTML;
      });
    }
  }

  openMedia(link: string) {
    window.open(link, '_blank');
  }
}
