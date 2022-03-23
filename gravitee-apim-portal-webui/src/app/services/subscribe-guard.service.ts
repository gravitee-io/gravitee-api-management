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
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';

import { CurrentUserService } from './current-user.service';
import { NotificationService } from './notification.service';

@Injectable({
  providedIn: 'root',
})
export class SubscribeGuardService {
  constructor(private currentUserService: CurrentUserService, private router: Router, private notificationService: NotificationService) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    const user = this.currentUserService.get().getValue();
    if (user == null) {
      const redirectUrl = state.url;
      this.router
        .navigate(['/user/login'], { replaceUrl: true, queryParams: { redirectUrl } })
        .then(() => this.notificationService.warning(i18n('apiSubscribe.errors.notConnected')));
      return false;
    }
    return true;
  }
}
