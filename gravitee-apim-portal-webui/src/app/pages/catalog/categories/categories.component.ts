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
import '@gravitee/ui-components/wc/gv-category-list';
import { ActivatedRoute, Router } from '@angular/router';

import { Category, PortalService } from '../../../../../projects/portal-webclient-sdk/src/lib';

@Component({
  selector: 'app-categories',
  templateUrl: './categories.component.html',
})
export class CategoriesComponent implements OnInit {
  nbCategories: object;
  categories: Array<Category>;
  empty: boolean;
  emptyIcon: any;

  constructor(private portalService: PortalService, private router: Router, private activatedRoute: ActivatedRoute) {}

  ngOnInit() {
    this.empty = false;
    this.categories = new Array(6).fill(null);
    this.portalService
      .getCategories({ size: -1 })
      .toPromise()
      .then(response => {
        this.nbCategories = response.metadata.data.total;
        this.categories = response.data;
        if (response.data.length === 0) {
          this.empty = true;
          this.emptyIcon = this.activatedRoute.snapshot.data.icon;
        }
      });
  }

  @HostListener(':gv-category:click', ['$event.detail'])
  onCardClick(category: Category) {
    this.router.navigate([`/catalog/categories/${category.id}`]);
  }
}
