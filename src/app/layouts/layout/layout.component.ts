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
import '@gravitee/ui-components/wc/gv-menu';
import '@gravitee/ui-components/wc/gv-nav';
import '@gravitee/ui-components/wc/gv-user-menu';
import { ActivatedRoute, NavigationEnd, Router, PRIMARY_OUTLET } from '@angular/router';
import { Link, User, PortalService } from '@gravitee/ng-portal-webclient';
import { Component, ComponentFactoryResolver, HostListener, OnInit, ViewChild } from '@angular/core';
import { CurrentUserService } from '../../services/current-user.service';
import { GvMenuRightSlotDirective } from '../../directives/gv-menu-right-slot.directive';
import { GvMenuTopSlotDirective } from '../../directives/gv-menu-top-slot.directive';
import { INavRoute, NavRouteService } from '../../services/nav-route.service';
import { Notification } from '../../model/notification';
import { NotificationService } from '../../services/notification.service';
import { TranslateService } from '@ngx-translate/core';
import { ConfigurationService } from '../../services/configuration.service';
import { FeatureEnum } from '../../model/feature.enum';

@Component({
  selector: 'app-layout',
  templateUrl: './layout.component.html'
})
export class LayoutComponent implements OnInit {

  public mainRoutes: Promise<INavRoute[]>;
  public userRoutes: Promise<INavRoute[]>;
  public menuRoutes: Promise<INavRoute[]>;
  public currentUser: User;
  public notification: Notification;
  public links: any = {};
  @ViewChild(GvMenuTopSlotDirective, { static: true }) appGvMenuTopSlot: GvMenuTopSlotDirective;
  @ViewChild(GvMenuRightSlotDirective, { static: true }) appGvMenuRightSlot: GvMenuRightSlotDirective;

  constructor(
    private translateService: TranslateService,
    private router: Router,
    private currentUserService: CurrentUserService,
    private navRouteService: NavRouteService,
    private notificationService: NotificationService,
    private activatedRoute: ActivatedRoute,
    private componentFactoryResolver: ComponentFactoryResolver,
    private configurationService: ConfigurationService,
    private portalService: PortalService
  ) {
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd) {
        this._onNavigationEnd();
      }
    });
  }


  async ngOnInit() {
    this.currentUserService.get().subscribe(newCurrentUser => {
      this.currentUser = newCurrentUser;
      this.userRoutes = this.navRouteService.getUserNav();
    });

    this.notificationService.notification.subscribe(notification => {
      if (notification) {
        this.translateService.get(notification.code, notification.parameters).subscribe((translatedMessage) => {
          if (notification.code !== translatedMessage || !notification.message) {
            notification.message = translatedMessage;
          }
          this.notification = notification;
        });
      } else {
        delete this.notification;
      }
    });
  }

  _buildLinks(links: Link[]): INavRoute[] {
    return links.map(element => {
      let path: string;
      let target: string;
      switch (element.resourceType) {
        case Link.ResourceTypeEnum.External:
          path = element.resourceRef;
          if (path.toLowerCase().startsWith('http')) {
            target = '_blank';
          } else {
            target = '_self';
          }
          break;
        case Link.ResourceTypeEnum.Page:
          if (element.folder) {
            path = '/documentation';
            if (element.resourceRef && element.resourceRef) {
              path += '/' + element.resourceRef;
            }
          } else {
            path = '/pages/' + element.resourceRef;
          }
          target = '_self';
          break;
        case Link.ResourceTypeEnum.View:
          path = '/categories/' + element.resourceRef;
          target = '_self';
          break;
      }
      const navRoute: INavRoute = {
        active : this.router.isActive(path, false),
        path,
        target,
        title: element.name
      };
      return navRoute;
    });
  }

  getUserName() {
    if (this.currentUser) {
      return this.currentUser.display_name;
    }
    return null;
  }

  @HostListener(':gv-nav:click', ['$event.detail'])
  @HostListener(':gv-link:click', ['$event.detail'])
  onNavChange(route: INavRoute) {
    if (route.target && route.target === '_blank') {
      window.open(route.path, route.target);
    } else {
      this.router.navigate([route.path]);
    }
  }

  private _onNavigationEnd() {
    this.portalService.getPortalLinks().subscribe((portalLinks) => {
      if (portalLinks.slots) {
        this.mainRoutes = this.navRouteService.getChildrenNav(this.activatedRoute);
        if (portalLinks.slots.header) {
          const headerLinks = portalLinks.slots.header.find(catLinks => catLinks.root);
          this.mainRoutes = this.mainRoutes.then(navRoutes => navRoutes.concat(this._buildLinks(headerLinks.links)));
        }
        if (portalLinks.slots.subfooter) {
          const subfooterLinks = portalLinks.slots.subfooter.find(catLinks => catLinks.root);
          this.links.subfooter = this._buildLinks(subfooterLinks.links);
        }

        if (portalLinks.slots.footer) {
          this.links.footer = portalLinks.slots.footer
            .filter( catLinks => !catLinks.root )
            .map(catLinks => {
              return {
                title: catLinks.category,
                links: this._buildLinks(catLinks.links)
              };
            });
        }
      }});

    const currentRoute: ActivatedRoute = this.navRouteService.findCurrentRoute(this.activatedRoute);
    this.menuRoutes = this.navRouteService.getSiblingsNav(currentRoute);
    if (this.menuRoutes) {
      const menuOption = currentRoute.snapshot.data.menu;
      if (typeof menuOption === 'object' && menuOption.slots) {
        this._injectComponent(this.appGvMenuTopSlot.viewContainerRef, menuOption.slots.top);
        this._injectComponent(this.appGvMenuRightSlot.viewContainerRef, menuOption.slots.right);
      } else {
        this._clearMenuSlots();
      }
    } else {
      this._clearMenuSlots();
    }
  }

  private _clearMenuSlots() {
    this.appGvMenuTopSlot.viewContainerRef.clear();
    this.appGvMenuRightSlot.viewContainerRef.clear();
  }

  private _injectComponent(viewContainerRef, slot) {
    if (slot && viewContainerRef.length === 0) {
      const componentFactory = this.componentFactoryResolver.resolveComponentFactory(slot);
      viewContainerRef.createComponent(componentFactory);
    } else if (slot == null) {
      viewContainerRef.clear();
    }
  }

  isHomepage(): boolean {
    return '/' === this.router.url;
  }

  isAuthenticated(): boolean {
    return this.currentUserService.get().getValue() !== null;
  }

  displaySignUp(): boolean {
    return this.configurationService.hasFeature(FeatureEnum.userRegistration) && !this.isAuthenticated();
  }

  displayShadowNav(): boolean {
    return this.isHomepage() && !this.isAuthenticated();
  }

  goTo(path: string) {
    this.router.navigate([path]);
  }
}
