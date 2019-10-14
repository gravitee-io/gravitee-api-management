import {Component} from '@angular/core';
import {APIService} from 'ng-portal-webclient/dist';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'Gravitee.io Portal';

  constructor(private apiService: APIService) {
    const apis = apiService.getApis();
    apis.subscribe(
      {
        next(x) { console.log('got value ' + x); },
        error(err) { console.error('something wrong occurred: ' + err); },
        complete() { console.log('done'); }
      }
    );
  }

}
