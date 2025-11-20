/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { ActivatedRoute, Router } from '@angular/router';
import { map, take } from 'rxjs';

import { PageService } from '../../../services/page.service';

@Component({
  selector: 'app-guides-redirect-to-first-id',
  standalone: true,
  template: '',
})
export class GuidesRedirectToFirstIdComponent implements OnInit {
  constructor(
    private readonly pageService: PageService,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.pageService
      .listByEnvironment()
      .pipe(
        map(pagesResponse => {
          const pages = (pagesResponse.data ?? []).filter(p => p.type !== 'LINK' && p.type !== 'FOLDER');
          return pages.length > 0 ? pages[0].id : null;
        }),
        take(1),
      )
      .subscribe(firstPageId => {
        if (firstPageId) {
          this.router.navigate(['.', firstPageId], {
            relativeTo: this.activatedRoute,
            replaceUrl: true,
          });
        }
      });
  }
}
