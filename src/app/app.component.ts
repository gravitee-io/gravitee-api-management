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
import '@gravitee/ui-components/wc/gv-message';
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
  OnDestroy, ChangeDetectorRef,
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Title } from '@angular/platform-browser';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { UserNotificationComponent } from './pages/user/user-notification/user-notification.component';
import { CurrentUserService } from './services/current-user.service';
import { NotificationService } from './services/notification.service';
import { ActivatedRoute, NavigationEnd, NavigationStart, Router, RouterOutlet } from '@angular/router';
import { INavRoute, NavRouteService } from './services/nav-route.service';
import { animation } from './route-animation';
import { Link, PortalService, User, UserService } from '@gravitee/ng-portal-webclient';
import { Notification } from './model/notification';
import { GvMenuTopSlotDirective } from './directives/gv-menu-top-slot.directive';
import { GvMenuRightTransitionSlotDirective } from './directives/gv-menu-right-transition-slot.directive';
import { ConfigurationService } from './services/configuration.service';
import { FeatureEnum } from './model/feature.enum';
import { GvMenuRightSlotDirective } from './directives/gv-menu-right-slot.directive';
import { GvSlot } from './directives/gv-slot';
import { GoogleAnalyticsService } from './services/google-analytics.service';
import { EventService, GvEvent } from './services/event.service';
import { ItemResourceTypeEnum } from './model/itemResourceType.enum';
import { PreviewService } from './services/preview.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  animations: [
    animation,
  ]
})
export class AppComponent implements AfterViewInit, OnInit, OnDestroy {

  static UPDATE_USER_AVATAR: ':gv-user:avatar';
  public mainRoutes: Promise<INavRoute[]>;
  public userRoutes: Promise<INavRoute[]>;
  public menuRoutes: Promise<INavRoute[]>;
  public currentUser: User;
  public userPicture: string;
  public notification: Notification;
  public isPreview = false;
  public isSticky = false;
  public isHomepage = false;
  public isStickyHomepage = false;
  public links: any = {};
  @ViewChild(GvMenuTopSlotDirective, { static: true }) appGvMenuTopSlot: GvMenuTopSlotDirective;

  @ViewChild(GvMenuRightTransitionSlotDirective, { static: true }) appGvMenuRightTransitionSlot: GvMenuRightTransitionSlotDirective;
  @ViewChild(GvMenuRightSlotDirective, { static: true }) appGvMenuRightSlot: GvMenuRightSlotDirective;
  @ViewChild('homepageBackground', { static: true }) homepageBackground;
  private slots: Array<GvSlot>;
  private homepageBackgroundHeight: number;
  private interval: any;
  public numberOfPortalNotifications: any;

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
    private userService: UserService,
    private eventService: EventService,
    private ref: ChangeDetectorRef,
    private googleAnalyticsService: GoogleAnalyticsService,
    private previewService: PreviewService
  ) {

    this.activatedRoute.queryParamMap.subscribe(params => {
      if (params.has('preview') && params.get('preview') === 'on') {
        this.previewService.activate();
      }
    });

    this.googleAnalyticsService.load();

    this.router.events.subscribe((event) => {
      if (event instanceof NavigationStart) {
        this.notificationService.reset();
      } else if (event instanceof NavigationEnd) {
        const currentRoute: ActivatedRoute = this.navRouteService.findCurrentRoute(this.activatedRoute);
        this._setBrowserTitle(currentRoute);
        this.isPreview = previewService.isActive();
        this._onNavigationEnd();
      }
    });
  }

  async ngOnInit() {
    this.currentUserService.get().subscribe(newCurrentUser => {
      this.currentUser = newCurrentUser;
      this.userRoutes = this.navRouteService.getUserNav();
      if (this.currentUser) {
        setTimeout(() => {
          this.userPicture = this.currentUser._links ? this.currentUser._links.avatar : null;
        });
        this.loadNotifications();
        this.interval = setInterval(
          () => {
            this.loadNotifications();
          },
          this.configurationService.get('scheduler.notificationsInSeconds') * 1000
        );
      } else {
        this.userPicture = null;
        clearInterval(this.interval);
      }
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
      this.ref.detectChanges();
    });
  }

  private loadNotifications() {
    this.userService.getCurrentUserNotifications({ size: 1 }).toPromise().then((response) => {
      if (response.data && response.data[0]) {
        const portalNotification = response.data[0];
        const total = response.metadata.pagination ? response.metadata.pagination.total : 0;
        if (this.numberOfPortalNotifications !== null && (total > this.numberOfPortalNotifications)) {
          this.eventService.dispatch(new GvEvent(UserNotificationComponent.NEW));
          const windowNotification = (window as any).Notification;
          if (windowNotification) {
            windowNotification.requestPermission()
              .then((permission) => {
                if (permission === 'granted') {
                  const n = new windowNotification(portalNotification.title, {
                    body: portalNotification.message
                  });
                }
              });
          }
        }
        this.numberOfPortalNotifications = total;
        this.ref.detectChanges();
      }
    });
  }

  ngAfterViewInit() {
    const loader = document.querySelector('#loader');
    if (loader) {
      loader.remove();
    }
    this.slots = [this.appGvMenuRightSlot, this.appGvMenuRightTransitionSlot, this.appGvMenuTopSlot];

    this.eventService.subscribe((event) => {
      if (event.type === UserNotificationComponent.REMOVE) {
        this.loadNotifications();
      } else if (event.type === AppComponent.UPDATE_USER_AVATAR) {
        this.userPicture = event.details.data;
      }
    });
  }

  onCloseNotification() {
    this.notificationService.reset();
  }

  @HostListener('window:beforeunload')
  async ngOnDestroy() {
    clearInterval(this.interval);
    this.eventService.unsubscribe();
  }

  @HostListener(':gv-theme:error', ['$event.detail'])
  onThemeError(detail) {
    this.notificationService.error(detail.message);
  }

  @HostListener('window:scroll')
  onScroll() {
    this.computeMenuMode();
  }

  @HostListener('window:resize')
  onResize() {
    this.computeHomepageHeight();
  }

  private computeMenuMode() {
    const pageYOffset = window.pageYOffset;
    window.requestAnimationFrame(() => {
      if (this.isHomepage) {
        this.isStickyHomepage = pageYOffset >= this.homepageBackgroundHeight - 70;
      }
      if (!this.isSticky) {
        this.isSticky = pageYOffset > 50;
      } else {
        this.isSticky = !(pageYOffset === 0);
      }
    });
  }

  private computeHomepageHeight() {
    if (this.isHomepage) {
      setTimeout(() => {
        if (this.homepageBackground.nativeElement) {
          this.homepageBackgroundHeight = parseInt(window.getComputedStyle(this.homepageBackground.nativeElement).height, 10);
        }
      }, 0);
    }
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

  get userName() {
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
    this.isHomepage = '/' === this.router.url;
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
        const parentSlots = currentRoute.parent && currentRoute.parent.snapshot.data.menu && currentRoute.parent.snapshot.data.menu.slots;
        this._injectMenuSlots({ ...menuOption.slots, ...parentSlots });
      } else {
        this._clearMenuSlots();
      }
    } else {
      this._clearMenuSlots();
    }
    this.computeMenuMode();
    this.computeHomepageHeight();
  }

  private _clearMenuSlots() {
    this.slots.forEach((directive) => {
      directive.clear();
    });
  }

  private _injectMenuSlots(slots) {
    this.slots.forEach((directive) => {
      const name = directive.getName();
      let slot = slots ? slots[name] : null;
      if (slots && slots.expectedFeature && !this.configurationService.hasFeature(slots.expectedFeature)) {
        slot = null;
      }
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

  isAuthenticated(): boolean {
    return this.currentUserService.get().getValue() !== null;
  }

  displaySignUp(): boolean {
    return this.configurationService.hasFeature(FeatureEnum.userRegistration) && !this.isAuthenticated();
  }

  displayShadowNav(): boolean {
    return this.isHomepage && !this.isAuthenticated();
  }

  goTo(path: string) {
    this.router.navigate([path]);
  }

  @HostListener(':gv-list:click', ['$event.detail'])
  onGvListclick(detail: any) {
    if (detail.type === ItemResourceTypeEnum.API) {
      this.goTo(`/catalog/api/${detail.item.id}`);
    } else if (detail.type === ItemResourceTypeEnum.APPLICATION) {
      this.goTo(`/applications/${detail.item.id}`);
    }
  }
}
