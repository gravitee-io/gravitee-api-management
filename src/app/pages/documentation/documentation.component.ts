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
import { Page, PortalService } from '@gravitee/ng-portal-webclient';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-documentation',
  templateUrl: './documentation.component.html',
})
export class DocumentationComponent implements OnInit {

  pages: Page[];
  rootDir: string;

  constructor(
    private portalService: PortalService,
    private activeRoute: ActivatedRoute
  ) { }

  ngOnInit() {
    this.activeRoute.params.subscribe((params) => {
      if (params.rootDir && params.rootDir !== 'root' ) {
        this.rootDir = params.rootDir;
        this.portalService.getPages({ homepage: false, size: -1, parent: this.rootDir })
          .subscribe(pagesResponse => { this.pages = pagesResponse.data; });
      } else {
        delete this.rootDir;
        this.portalService.getPages({ homepage: false, size: -1 })
          .subscribe(pagesResponse => { this.pages = pagesResponse.data; });
      }
    });
  }
}
