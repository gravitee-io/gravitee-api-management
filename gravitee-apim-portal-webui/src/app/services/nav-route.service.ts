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
import { ActivatedRoute, NavigationExtras, Route, Router, Routes } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { FeatureGuardService } from './feature-guard.service';
import { AuthGuardService } from './auth-guard.service';
import { CurrentUserService } from './current-user.service';
import { PermissionGuardService } from './permission-guard.service';

export interface INavRoute {
  path: string;
  title: string;
  icon?: string;
  active?: boolean;
  separator?: boolean;
  target?: string;
}

@Injectable({
  providedIn: 'root',
})
export class NavRouteService {
  constructor(
    private router: Router,
    private translateService: TranslateService,
    private featureGuardService: FeatureGuardService,
    private currentUserService: CurrentUserService,
    private authGuardService: AuthGuardService,
    private permissionGuardService: PermissionGuardService,
  ) {}

  async getUserNav(): Promise<INavRoute[]> {
    const parentPath = 'user';
    const userRoute = this.getRouteByPath(parentPath);
    const managementRoute: Promise<INavRoute> = this.getManagementNav();
    const userRoutes: Promise<INavRoute[]> = this.getChildrenNav(userRoute, parentPath, []);

    return Promise.all([managementRoute, userRoutes]).then(values => {
      const routesArray = values[1];

      if (routesArray.length > 1 && values[0]) {
        const logoutRoute = routesArray.pop();
        routesArray.push(values[0]);
        routesArray.push(logoutRoute);
      }

      return routesArray;
    });
  }

  async getManagementNav(): Promise<INavRoute> {
    const user = this.currentUserService.getUser();
    if (user && user.config && user.config.management_url) {
      return this.translateService
        .get('route.management')
        .toPromise()
        .then(_title => {
          const routeNav: INavRoute = {
            path: user.config.management_url,
            icon: 'code:settings',
            title: _title,
            target: '_blank',
          };
          return routeNav;
        });
    }
  }

  async getChildrenNav(aRoute: ActivatedRoute | Route, parentPath?: string, hiddenPaths?: Array<string>): Promise<INavRoute[]> {
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    const _route: { data; pathFromRoot; routeConfig; children; path } = aRoute instanceof ActivatedRoute ? aRoute.snapshot : aRoute;

    const data = _route.data;
    if (data && data.menu) {
      const menuOptions = typeof data.menu === 'object' ? data.menu : { hiddenPaths: [] };
      const _hiddenPaths = (hiddenPaths ? hiddenPaths : menuOptions.hiddenPaths) || [];

      const _parentPath = parentPath
        ? parentPath
        : (_route.pathFromRoot || [])
            .filter(route => route.routeConfig)
            .map(route => route.routeConfig.path)
            .join('/');

      let children = _route.routeConfig ? _route.routeConfig.children : _route.children;
      if (_route.routeConfig && _route.routeConfig.loadChildren) {
        children = _route.routeConfig._loadedConfig.routes;
      }

      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      return Promise.all(
        children
          .filter(child => child.data != null && child.data.title)
          .filter(this.isVisiblePath(_hiddenPaths))
          .map(child => {
            const newChild = Object.assign({}, child);
            newChild.data = { ...data, ...child.data };
            return newChild;
          })
          .filter(child => this.featureGuardService.canActivate(child) === true)
          .filter(child => this.permissionGuardService.canActivate(child) === true)
          .map(async child => {
            const hasAuth = await this.authGuardService.canActivate(child);
            if (hasAuth === true) {
              let path = `${_parentPath}/${child.path}`;
              // remove trailing slash to allow empty path
              if (path.endsWith('/')) {
                path = path.substring(0, path.length - 1);
              }
              const active = this.router.isActive(path, false) || this.isActive(path, this.router.url);
              return this.translateService
                .get(child.data.title)
                .toPromise()
                .then(_title => {
                  const routeNav: INavRoute = {
                    path,
                    icon: child.data.icon,
                    title: _title,
                    active,
                    separator: child.data.separator,
                  };
                  return routeNav;
                });
            }
            return null;
          }),
      ).then(routes => routes.filter(route => route != null));
    }
    return null;
  }

  private isActive(path, url) {
    const regexp = '^' + path.replace(/\/:([^/])+/, '/([^/])+') + '$';
    return new RegExp(regexp).test(url.substring(1).split('?')[0].split('#')[0]);
  }

  private isVisiblePath(_hiddenPaths) {
    return child => !_hiddenPaths.includes(child.path);
  }

  async getSiblingsNav(activatedRoute: ActivatedRoute): Promise<INavRoute[]> {
    const data = activatedRoute.snapshot.data;
    if (data.menu && !data.menu.hide) {
      const params = activatedRoute.snapshot.params;
      const childrenNav = this.getChildrenNav(activatedRoute.parent);
      if (params) {
        // Replace dynamic path param
        return childrenNav.then(navRoutes => {
          if (navRoutes) {
            return navRoutes.map(navRoute => {
              for (const key of Object.keys(params)) {
                navRoute.active = this.isActive(navRoute.path, this.router.url);
                navRoute.path = navRoute.path.replace(`:${key}`, params[key]);
              }
              return navRoute;
            });
          }
        });
      }
      return childrenNav;
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
      const found = children.find(route => route.path === path);
      if (found == null) {
        return children.map(route => this._getRouteByPath(route.children, path)).find(route => route && route.path === path);
      }
      return found;
    }
    return null;
  }

  navigateForceRefresh(commands: any[], extras?: NavigationExtras) {
    this.router
      .navigate([], {
        ...extras,
        ...{
          queryParams: { skipRefresh: true },
          skipLocationChange: true,
        },
      })
      .then(() => {
        if (extras && extras.queryParams) {
          extras.queryParams.skipRefresh = null;
        }
        this.router.navigate(commands, extras);
      });
  }
}
