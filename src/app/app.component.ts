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
import '@gravitee/ui-components/wc/gv-theme';
import {
  AfterViewInit,
  Component,
  ComponentFactoryResolver,
  HostListener,
  OnInit,
  ViewChild,
  OnDestroy,
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Title } from '@angular/platform-browser';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { CurrentUserService } from './services/current-user.service';
import { NotificationService } from './services/notification.service';
import { ActivatedRoute, NavigationEnd, NavigationStart, Router, RouterOutlet } from '@angular/router';
import { INavRoute, NavRouteService } from './services/nav-route.service';
import { animation } from './route-animation';
import { Link, PortalService, User } from '@gravitee/ng-portal-webclient';
import { Notification } from './model/notification';
import { GvMenuTopSlotDirective } from './directives/gv-menu-top-slot.directive';
import { GvMenuInputSlotDirective } from './directives/gv-menu-input-slot.directive';
import { ConfigurationService } from './services/configuration.service';
import { FeatureEnum } from './model/feature.enum';
import { GvMenuButtonSlotDirective } from './directives/gv-menu-button-slot.directive';
import { GvSlot } from './directives/gv-slot';

// for google analytics
declare var gtag;

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  animations: [
    animation,
  ]
})
export class AppComponent implements AfterViewInit, OnInit, OnDestroy {

  public mainRoutes: Promise<INavRoute[]>;
  public userRoutes: Promise<INavRoute[]>;
  public menuRoutes: Promise<INavRoute[]>;
  public currentUser: User;
  public notification: Notification;
  public isPreview = false;
  public links: any = {};
  @ViewChild(GvMenuTopSlotDirective, { static: true }) appGvMenuTopSlot: GvMenuTopSlotDirective;
  @ViewChild(GvMenuInputSlotDirective, { static: true }) appGvMenuRightSlot: GvMenuInputSlotDirective;
  @ViewChild(GvMenuButtonSlotDirective, { static: true }) appGvMenuButtonSlot: GvMenuButtonSlotDirective;
  private slots: Array<GvSlot>;

  constructor(
    private titleService: Title,
    private translateService: TranslateService,
    private router: Router,
    private currentUserService: CurrentUserService,
    private navRouteService: NavRouteService,
    private notificationService: NotificationService,
    private activatedRoute: ActivatedRoute,
    private componentFactoryResolver: ComponentFactoryResolver,
    private configurationService: ConfigurationService,
    private portalService: PortalService,
  ) {
    // google analytics
    const gaEnabled = this.configurationService.hasFeature(FeatureEnum.googleAnalytics);
    const gaTrackingId = this.configurationService.get('portal.analytics.trackingId');
    if (gaEnabled && gaTrackingId) {
      const scriptGA = document.createElement('script');
      scriptGA.async = true;
      scriptGA.src = 'https://www.googletagmanager.com/gtag/js?id=' + gaTrackingId;

      const scriptGtag = document.createElement('script');
      scriptGtag.async = true;
      scriptGtag.innerHTML = 'function gtag(){dataLayer.push(arguments)}window.dataLayer=window.dataLayer||[],gtag("js",new Date);';

      document.head.append(scriptGA);
      document.head.append(scriptGtag);
    }

    this.activatedRoute.queryParamMap.subscribe(params => {
      if (params.has('preview') && params.get('preview') === 'on') {
        sessionStorage.setItem('gvPreview', 'true');
      }
    });

    this.router.events.subscribe((event) => {
      if (event instanceof NavigationStart) {
        this.notificationService.reset();
      } else if (event instanceof NavigationEnd) {
        // google analytics
        if (gaEnabled && gaTrackingId) {
          gtag('config', gaTrackingId, { page_path: event.urlAfterRedirects, cookieDomain: 'none' });
        }

        const currentRoute: ActivatedRoute = this.navRouteService.findCurrentRoute(this.activatedRoute);
        this._setBrowserTitle(currentRoute);
        const gvPreview = sessionStorage.getItem('gvPreview');
        if (gvPreview) {
          this.isPreview = true;
          this.notificationService.info('On preview mode');
        }
        this._onNavigationEnd();
      }
    });
  }

  @HostListener('window:beforeunload')
  async ngOnDestroy() {
    sessionStorage.removeItem('gvPreview');
  }

  @HostListener(':gv-theme:error', ['$event.detail'])
  onThemeError(detail) {
    this.notificationService.error(detail.message);
  }

  ngAfterViewInit() {
    const loader = document.querySelector('#loader');
    if (loader) {
      loader.remove();
    }
    this.slots = [this.appGvMenuButtonSlot, this.appGvMenuRightSlot, this.appGvMenuTopSlot];
  }

  prepareRoute(outlet: RouterOutlet) {
    return outlet && outlet.activatedRouteData && outlet.activatedRouteData.animation;
  }

  private _setBrowserTitle(currentRoute: ActivatedRoute) {
    this.translateService.get(i18n('site.title')).subscribe((siteTitle) => {
      const data = currentRoute.snapshot.data;
      if (data && data.title) {
        this.translateService.get(data.title).subscribe((title) => this.titleService.setTitle(`${title} | ${siteTitle}`));
      } else {
        this.titleService.setTitle(siteTitle);
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
          setTimeout(() => this.notification = notification);
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
          if (path && path.toLowerCase().startsWith('http')) {
            target = '_blank';
          } else {
            target = '_self';
          }
          break;
        case Link.ResourceTypeEnum.Page:
          if (element.folder) {
            path = '/documentation';
            if (element.resourceRef) {
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
        active: this.router.isActive(path, false),
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

        this.mainRoutes = this.navRouteService.getChildrenNav(
          {
            data: { menu: true },
            children: this.router.config.filter((route) => (route.data && route.data.menu))
          }
        );

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
            .filter(catLinks => !catLinks.root)
            .map(catLinks => {
              return {
                title: catLinks.category,
                links: this._buildLinks(catLinks.links)
              };
            });
        }
      }
    });

    const currentRoute: ActivatedRoute = this.navRouteService.findCurrentRoute(this.activatedRoute);
    this.menuRoutes = this.navRouteService.getSiblingsNav(currentRoute);
    if (this.menuRoutes) {
      const menuOption = currentRoute.snapshot.data.menu;
      if (typeof menuOption === 'object') {
        this._injectMenuSlots(menuOption.slots);
      } else {
        this._clearMenuSlots();
      }
    } else {
      this._clearMenuSlots();
    }
  }

  private _clearMenuSlots() {
    this.slots.forEach((directive) => {
      directive.clear();
    });
  }

  private _injectMenuSlots(slots) {
    this.slots.forEach((directive) => {
      const name = directive.getName();
      const slot = slots ? slots[name] : null;
      this._updateSlot(slot, directive);
    });
  }

  private _updateSlot(slot, directive) {
    if (slot == null) {
      directive.clear();
    } else {
      const componentFactory = this.componentFactoryResolver.resolveComponentFactory(slot);
      directive.setComponent(componentFactory);
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
