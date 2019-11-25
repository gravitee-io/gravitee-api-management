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
import { PortalService, View } from '@gravitee/ng-portal-webclient';

import '@gravitee/ui-components/wc/gv-card-category';


@Component({
  selector: 'app-categories',
  templateUrl: './categories.component.html',
  styleUrls: ['./categories.component.css']
})
export class CategoriesComponent implements OnInit {
  nbCategories: string;
  categories: View[];

  constructor(
    private portalService: PortalService,
  ) {
  }

  ngOnInit() {

    this.portalService.getViews({}).subscribe(
      (viewResponse) => {
        this.categories = viewResponse.data;
        this.nbCategories = viewResponse.metadata.data.total;
      }
    );
  }

  getCategoryBackgroundColor(index) {
    return `--gv-card-category--bgc: var(--gv-theme-color-category-${index % 6 + 1})`;
  }

  searchAPIsByCategory(categoryId: string) {
    console.log('calling /apis?views=' + categoryId);
  }
}
