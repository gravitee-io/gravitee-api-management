import {Component, HostListener, OnInit} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {environment} from '../environments/environment';

import '@gravitee/ui-components/wc/gv-header';

import {Title} from '@angular/platform-browser';
import {explicitRoutes} from './app-routing.module';
import {marker as i18n} from '@biesbjerg/ngx-translate-extract-marker';
import {Router} from '@angular/router';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {

  private routes: Promise<any>[];

  constructor(private titleService: Title, private translateService: TranslateService, private router: Router) {
  }

  ngOnInit() {
    this.translateService.addLangs(environment.locales);
    this.translateService.setDefaultLang(environment.locales[0]);
    const browserLang = this.translateService.getBrowserLang();
    this.translateService.use(browserLang.match(/en|fr/) ? browserLang : 'en');

    this.translateService.get(i18n('site.title')).subscribe((title) => {
      this.titleService.setTitle(title);
    });

    this.routes = explicitRoutes.map((route) => {
      return this.translateService.get(route.title).toPromise().then((title) => {
        route.title = title;
        if (`/${route.path}` === this.router.url) {
          // @ts-ignore
          route.isActive = true;
        }
        return route;
      });
    });
  }

  @HostListener('gv-nav_change', ['$event.detail'])
  onNavChange(route) {
    this.router.navigate([route.path]);
  }

}
