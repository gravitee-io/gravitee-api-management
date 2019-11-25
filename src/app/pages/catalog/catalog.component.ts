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
import { Component, OnInit, HostListener } from '@angular/core';
import { RouteService, RouteType } from '../../services/route.service';
import '@gravitee/ui-components/wc/gv-menu';
import { Router } from '@angular/router';
import { ApiService } from '@gravitee/ng-portal-webclient';

@Component({
  selector: 'app-catalog',
  templateUrl: './catalog.component.html',
  styleUrls: ['./catalog.component.css']
})
export class CatalogComponent implements OnInit {

  public catalogRoutes: object[];

  constructor(private routeService: RouteService, private router: Router, private apiService: ApiService) {
    this.catalogRoutes = [];
  }

  async ngOnInit() {
    this.catalogRoutes = await this.routeService.getRoutes(RouteType.catalog).map(async (route) => {
      if (route.categoryApiQuery) {
        return this.apiService.getApis({ cat: route.categoryApiQuery }).toPromise().then(async ({ metadata: { data: { total } } }) => {
          if (parseInt(total, 10) === 0) {
            throw new Error('This route should not be displayed');
          }
          // @ts-ignore
          route.help = total;
          return route;
        });
      }
      return route;
    });

  }

  @HostListener(':gv-input:submit', ['$event.detail'])
  onSearchInput(queryInput: string) {
    this.router.navigate(['/catalog/search'], { queryParams: { q: queryInput} });
  }
}
