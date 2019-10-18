import {Component} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {environment} from '../environments/environment';

import 'node_modules/@gravitee/components/dist/atoms/gv-input';
import {Title} from '@angular/platform-browser';

import {marker as i18n} from '@biesbjerg/ngx-translate-extract-marker';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {

  constructor(private titleService: Title, private translate: TranslateService) {
    translate.addLangs(environment.locales);
    translate.setDefaultLang(environment.locales[0]);

    const browserLang = translate.getBrowserLang();
    translate.use(browserLang.match(/en|fr/) ? browserLang : 'en');

    translate.get(i18n('site.title')).subscribe({
      next(e) {
        titleService.setTitle(e);
      }
    });


  }


}
