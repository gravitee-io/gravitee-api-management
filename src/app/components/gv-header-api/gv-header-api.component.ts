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
import '@gravitee/ui-components/wc/gv-header';
import { ActivatedRoute, Router } from '@angular/router';
import { Api, ApiService, User } from '@gravitee/ng-portal-webclient';
import { Component, OnInit } from '@angular/core';
import { CurrentUserService } from '../../services/current-user.service';
import { NavRouteService } from '../../services/nav-route.service';

@Component({
    selector: 'app-gv-header-api',
    templateUrl: './gv-header-api.component.html'
})
export class GvHeaderApiComponent implements OnInit {
    public api: Promise<Api>;
    public currentUser: User;
    private apiId: string;
    private currentRoute: ActivatedRoute;
    private _subscribeUrl: string;

    constructor(public router: Router,
                public activatedRoute: ActivatedRoute,
                public navRouteService: NavRouteService,
                public currentUserService: CurrentUserService,
                public apiService: ApiService) {
    }

    ngOnInit() {
        this.currentRoute = this.navRouteService.findCurrentRoute(this.activatedRoute);
        if (this.currentRoute) {
            const params = this.currentRoute.snapshot.params;
            this.apiId = params.apiId ? params.apiId : '?';
            this._subscribeUrl = `catalog/api/${ this.apiId }/subscribe`;
            this.api = this.apiService
                .getApiByApiId({ apiId: this.apiId })
                .toPromise()
                .catch((err) => Promise.reject(err));
        }

        this.currentUserService.get().subscribe(newCurrentUser => {
            this.currentUser = newCurrentUser;
        });

    }

    canSubscribe() {
        return !this.router.isActive(this._subscribeUrl, true);
    }

    onSubscribe() {
        this.router.navigate([this._subscribeUrl]);
    }

}
