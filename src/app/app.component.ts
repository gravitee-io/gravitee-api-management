import {Component, HostListener, OnInit} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {environment} from '../environments/environment';

import '@gravitee/ui-components/wc/gv-header';

import {Title} from '@angular/platform-browser';
import {routes, userRoutes} from './app-routing.module';
import {marker as i18n} from '@biesbjerg/ngx-translate-extract-marker';
import {Router} from '@angular/router';
import {User } from '@gravitee/clients-sdk/dist';
import { UserComponent } from './user/user.component';
import { CurrentUserService } from './currentUser.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {

  private routes: Promise<any>[];
  private userRoutes: Promise<any>[];
  private currentUser: User;
  constructor(
    private titleService: Title, 
    private translateService: TranslateService, 
    private router: Router,
    private currentUserService: CurrentUserService
    ) {
  }

  ngOnInit() {
    this.currentUserService.currentUser.subscribe(newCurrentUser => this.currentUser = newCurrentUser);
    
    this.translateService.addLangs(environment.locales);
    this.translateService.setDefaultLang(environment.locales[0]);
    const browserLang = this.translateService.getBrowserLang();
    this.translateService.use(browserLang.match(/en|fr/) ? browserLang : 'en');

    this.translateService.get(i18n('site.title')).subscribe((title) => {
      this.titleService.setTitle(title);
    });

    this.routes = routes.map((route) => {
      return this.translateService.get(route.title).toPromise().then((title) => {
        route.title = title;
        if (`/${route.path}` === this.router.url) {
          // @ts-ignore
          route.isActive = true;
        }
        return route;
      });
    });

    this.userRoutes = userRoutes.map((route) => {
      return this.translateService.get(route.title).toPromise().then((title) => {
        route.title = title;
        return route;
      });
    });
  }

  @HostListener('gv-nav_change', ['$event.detail'])
  @HostListener('gv-nav-link_click', ['$event.detail'])
  onNavChange(route) {
    this.router.navigate([route.path]);
  }

}
