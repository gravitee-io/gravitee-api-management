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
import { Component, HostListener, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { Page, PortalService } from '../../../../projects/portal-webclient-sdk/src/lib';

@Component({
  selector: 'app-single-page',
  templateUrl: './single-page.component.html',
  styleUrls: ['./single-page.component.css'],
})
export class SinglePageComponent implements OnInit {
  singlePage: Page;

  constructor(private route: ActivatedRoute, private router: Router, private portalService: PortalService) {}

  ngOnInit() {
    this.route.params.subscribe(params => {
      if (params.pageId) {
        this.portalService.getPageByPageId({ pageId: params.pageId }).subscribe(response => (this.singlePage = response));
      }
    });
  }

  @HostListener(':gv-button:click', ['$event.srcElement.dataset.pageId'])
  onInternalLinkClick(pageId: string) {
    this.router.navigate(['/documentation/root'], { queryParams: { page: pageId } });
  }
}
