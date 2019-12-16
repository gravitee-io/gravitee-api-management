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
import '@gravitee/ui-components/wc/gv-menu';
import '@gravitee/ui-components/wc/gv-nav';
import '@gravitee/ui-components/wc/gv-user-menu';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { ApiService, User } from '@gravitee/ng-portal-webclient';
import { Component, ComponentFactoryResolver, HostListener, OnInit, ViewChild } from '@angular/core';
import { CurrentUserService } from '../../services/current-user.service';
import { GvMenuRightSlotDirective } from '../../directives/gv-menu-right-slot.directive';
import { GvMenuTopSlotDirective } from '../../directives/gv-menu-top-slot.directive';
import { INavRoute, NavRouteService } from '../../services/nav-route.service';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { Notification } from '../../model/notification';
import { NotificationService } from '../../services/notification.service';
import { Title } from '@angular/platform-browser';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-layout',
  templateUrl: './layout.component.html'
})
export class LayoutComponent implements OnInit {

  public mainRoutes: Promise<any>[];
  public userRoutes: Promise<any[]>;
  public menuRoutes: Promise<any>[];
  public currentUser: User;
  public notification: Notification;
  public links: any;
  @ViewChild(GvMenuTopSlotDirective, { static: true }) appGvMenuTopSlot: GvMenuTopSlotDirective;
  @ViewChild(GvMenuRightSlotDirective, { static: true }) appGvMenuRightSlot: GvMenuRightSlotDirective;

  constructor(
    private titleService: Title,
    private translateService: TranslateService,
    private router: Router,
    private currentUserService: CurrentUserService,
    private navRouteService: NavRouteService,
    private notificationService: NotificationService,
    private activatedRoute: ActivatedRoute,
    private apiService: ApiService,
    private componentFactoryResolver: ComponentFactoryResolver
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
      // @ts-ignore
      this.mainRoutes = this.navRouteService.getChildrenNav(this.activatedRoute);
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

    this.links = {
      categorized: [
        {
          title: i18n('footer.categorized.catalog.title'), links: [
            { title: i18n('footer.categorized.catalog.links.moreInfo'), link: '/' },
            { title: i18n('footer.categorized.catalog.links.products'), link: '/' },
            { title: i18n('footer.categorized.catalog.links.documentations'), link: '/' },
          ]
        },
        {
          title: i18n('footer.categorized.help.title'), links: [
            { title: i18n('footer.categorized.help.links.contact'), link: '/' },
            { title: i18n('footer.categorized.help.links.support'), link: '/' },
            { title: i18n('footer.categorized.help.links.faq'), link: '/' },
          ]
        },
        {
          title: i18n('footer.categorized.resources.title'), links: [
            { title: i18n('footer.categorized.resources.links.news'), link: '/' },
            { title: i18n('footer.categorized.resources.links.blog'), link: '/' },
            { title: i18n('footer.categorized.resources.links.ebooks'), link: '/' },
            { title: i18n('footer.categorized.resources.links.events'), link: '/' },
          ]
        },
        {
          title: i18n('footer.categorized.howItWorks.title'), links: [
            { title: i18n('footer.categorized.howItWorks.links.news'), link: '/' },
            { title: i18n('footer.categorized.howItWorks.links.blog'), link: '/' },
            { title: i18n('footer.categorized.howItWorks.links.ebooks'), link: '/' },
            { title: i18n('footer.categorized.howItWorks.links.events'), link: '/' },
          ]
        },
      ],
      global: [
        { title: i18n('footer.links.mentions'), link: '/' },
        { title: i18n('footer.links.cgu'), link: '/' },
        { title: i18n('footer.links.cookies'), link: '/' },
        { title: i18n('footer.links.rgpd'), link: '/' },
        { title: i18n('footer.links.status'), link: '/' },
      ]
    };
  }

  getUserName() {
    if (this.currentUser) {
      return this.currentUser.display_name;
    }
    return null;
  }

  @HostListener(':gv-nav-link:click', ['$event.detail'])
  onNavChange(route: INavRoute) {
    this.router.navigate([route.path]);
  }

  private _onNavigationEnd() {
    // @ts-ignore
    this.mainRoutes = this.navRouteService.getChildrenNav(this.activatedRoute);
    const currentRoute: ActivatedRoute = this.navRouteService.findCurrentRoute(this.activatedRoute);
    // @ts-ignore
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
}
