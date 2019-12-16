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
import '@gravitee/ui-components/wc/gv-header-api';
import { ActivatedRoute, Router } from '@angular/router';
import { Api, ApiService, User } from '@gravitee/ng-portal-webclient';
import { Component, OnInit } from '@angular/core';
import { ContactComponent } from '../../pages/contact/contact.component';
import { CurrentUserService } from '../../services/current-user.service';
import { INavRoute, NavRouteService } from '../../services/nav-route.service';

@Component({
  selector: 'app-gv-menu-header',
  templateUrl: './gv-menu-header.component.html'
})
export class GvMenuHeaderComponent implements OnInit {
  public breadcrumbsHeader: Promise<INavRoute[]>;
  public api: Promise<Api>;
  public currentUser: User;

  constructor(public router: Router,
              public activatedRoute: ActivatedRoute,
              public navRouteService: NavRouteService,
              public currentUserService: CurrentUserService,
              public apiService: ApiService) {
  }

  ngOnInit() {
    this.breadcrumbsHeader = this.navRouteService.getBreadcrumbs(this.activatedRoute);
    const currentRoute: ActivatedRoute = this.navRouteService.findCurrentRoute(this.activatedRoute);
    if (currentRoute) {
      const params = currentRoute.snapshot.params;
      const apiId = params.apiId ? params.apiId : '?';
      this.api = this.apiService
        .getApiByApiId({ apiId })
        .toPromise()
        .catch((err) => Promise.reject(err));
    }

    this.currentUserService.get().subscribe(newCurrentUser => {
      this.currentUser = newCurrentUser;
    });
  }

  goToContact(api: Promise<Api>) {
    api.then((_api) => {
      const queryParams = {};
      queryParams[ContactComponent.API_QUERY_PARAM] = _api.id;
      this.router.navigate(['/user/contact'], { queryParams });
    });
  }

  canSubscribe() {
    return this.currentUser != null;
  }
}
