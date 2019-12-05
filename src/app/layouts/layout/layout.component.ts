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
import { Component, HostListener, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { TranslateService } from '@ngx-translate/core';
import { ActivatedRoute, NavigationEnd, Router, RoutesRecognized } from '@angular/router';
import { CurrentUserService } from '../../services/current-user.service';
import { RouteService } from '../../services/route.service';
import { User } from '@gravitee/ng-portal-webclient';
import '@gravitee/ui-components/wc/gv-nav';
import '@gravitee/ui-components/wc/gv-user-menu';
import '@gravitee/ui-components/wc/gv-menu';
import { NotificationService } from '../../services/notification.service';
import { Notification } from '../../model/notification';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';

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
  public hasSearch: boolean;
  menuSmall: boolean;

  constructor(
    private titleService: Title,
    private translateService: TranslateService,
    private router: Router,
    private currentUserService: CurrentUserService,
    private routeService: RouteService,
    private notificationService: NotificationService,
    private activatedRoute: ActivatedRoute
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
      this.userRoutes = this.routeService.getUserNav();
    });

    this.currentUserService.get().subscribe(newCurrentUser => this.currentUser = newCurrentUser);
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
  onNavChange(route: { path: any; }) {
    this.router.navigate([route.path]);
  }

  private _onNavigationEnd() {
    // @ts-ignore
    this.mainRoutes = this.routeService.getChildrenNav(this.activatedRoute);
    const currentRoute: ActivatedRoute = this.routeService.findCurrentRoute(this.activatedRoute);
    this.hasSearch = false;
    // @ts-ignore
    this.menuRoutes = this.routeService.getSiblingsNav(currentRoute);

    if (this.menuRoutes) {
      if (currentRoute.snapshot.data.menu) {
        this.menuSmall = currentRoute.snapshot.data.menu.small;
      } else {
        this.menuSmall = false;
      }
      const menuOptions = currentRoute.snapshot.parent.data.menu;
      if (typeof menuOptions === 'object' && menuOptions.slots) {
        this.hasSearch = menuOptions.slots.right != null;
      }
    }
  }

  onSearchInput({ detail }) {
    this.router.navigate(['/catalog/search'], { queryParams: { q: detail } });
  }
}
