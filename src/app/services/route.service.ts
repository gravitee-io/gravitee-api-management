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
import { Injectable } from '@angular/core';
import { ActivatedRoute, Route, Router, Routes } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { FeatureGuardService } from './feature-guard.service';
import { AuthGuardService } from './auth-guard.service';

@Injectable({
  providedIn: 'root'
})
export class RouteService {

  constructor(private router: Router,
              private translateService: TranslateService,
              private featureGuardService: FeatureGuardService,
              private authGuardService: AuthGuardService) {
  }

  async getUserNav() {
    const parentPath = 'user';
    const userRoute = this.getRouteByPath(parentPath);
    return this.getChildrenNav(userRoute, parentPath, []);
  }

  async getChildrenNav(aRoute: ActivatedRoute | Route, parentPath?: string, hiddenPaths?: Array<string>) {
    // @ts-ignore
    const _route: { data, pathFromRoot, routeConfig, children, path } = aRoute instanceof ActivatedRoute ? aRoute.snapshot : aRoute;

    const data = _route.data;
    if (data && data.menu) {
      const menuOptions = typeof data.menu === 'object' ? data.menu : { hiddenPaths: [] };
      const _hiddenPaths = (hiddenPaths ? hiddenPaths : menuOptions.hiddenPaths) || [];

      const _parentPath = parentPath ? parentPath : (_route.pathFromRoot || [])
        .filter((route) => route.routeConfig)
        .map((route) => route.routeConfig.path).join('/');

      const children = _route.routeConfig ? _route.routeConfig.children : _route.children;
      // @ts-ignore
      return Promise.all(children
      // @ts-ignore
        .filter((child) => child.data != null && child.data.title)
        .filter(this.isVisiblePath(_hiddenPaths))
        .filter((child) => this.featureGuardService.canActivate(child) === true)
        .map(async (child) => {
          const hasAuth = await this.authGuardService.canActivate(child);
          if (hasAuth) {
            const path = `${ _parentPath }/${ child.path }`;
            const active = this.router.isActive(path, false);
            return this.translateService.get(child.data.title).toPromise().then((_title) => {
              return {
                path,
                icon: child.data.icon,
                title: _title,
                active
              };
            });
          }
          return null;
        }))
        .then((routes) => routes.filter((route) => route != null));
    }
    return null;
  }

  private isVisiblePath(_hiddenPaths) {
    return (child) => {
      const path = child.path ? child.path.split(':')[0] : child.path;
      return !_hiddenPaths.includes(path);
    };
  }

  async getSiblingsNav(activatedRoute: ActivatedRoute) {
    const data = activatedRoute.snapshot.data;
    if (data.menu) {
      return this.getChildrenNav(activatedRoute.parent);
    }
    return null;
  }

  findCurrentRoute(activatedRoute: ActivatedRoute) {
    let route = activatedRoute.firstChild;
    let child = route;

    while (child) {
      if (child.firstChild) {
        child = child.firstChild;
        route = child;
      } else {
        child = null;
      }
    }
    return route;
  }

  getRouteByPath(path: string) {
    return this._getRouteByPath(this.router.config, path);
  }

  _getRouteByPath(children: Routes, path: string) {
    if (children) {
      const found = children.find((route) => route.path === path);
      if (found == null) {
        return children.map((route) => this._getRouteByPath(route.children, path))
          .find((route) => route && route.path === path);
      }
      return found;
    }
    return null;
  }

}
