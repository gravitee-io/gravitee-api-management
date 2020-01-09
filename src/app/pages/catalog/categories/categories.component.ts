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
import { Router } from '@angular/router';
import { delay } from '../../../utils/utils';
import { TimeTooLongError } from '../../../exceptions/TimeTooLongError';

@Component({
  selector: 'app-categories',
  templateUrl: './categories.component.html',
  styleUrls: ['./categories.component.css']
})
export class CategoriesComponent implements OnInit {
  nbCategories: number;
  categories: Array<View>;

  constructor(
    private portalService: PortalService,
    private router: Router
  ) {
  }

  ngOnInit() {

    Promise.race([this._loadCards(), delay(500)])
      .catch((err) => {
        if (err instanceof TimeTooLongError) {
          // @ts-ignore
          this.categories = new Array<View>(6);
          // @ts-ignore
          this.nbCategories = 6;
        }
      });
  }

  private _loadCards() {
    return this.portalService.getViews({ size: -1 }).toPromise().then((response) => {
      // @ts-ignore
      this.categories = response.data;
      // @ts-ignore
      this.nbCategories = response.metadata.data.total;
    });
  }

  getCategoryBackgroundColor(index) {
    return `--gv-card-category--bgc: var(--gv-theme-color-category-${ index % 6 + 1 })`;
  }

  async onCardClick(category: Promise<View>) {
    const view = await category;
    await this.router.navigate([`/catalog/categories/${ view.id }`]);
  }

}
