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
import { Injectable, NgModule } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

import { User, UserService, UsersService } from '../../../projects/portal-webclient-sdk/src/lib';
import { CurrentUserService } from '../services/current-user.service';

@Injectable()
export class UserServiceStub {}

@Injectable()
export class UsersServiceStub {}

@Injectable()
export class CurrentUserServiceStub {
  get(): BehaviorSubject<User> {
    return new BehaviorSubject<User>({});
  }
}

@NgModule({
  declarations: [],
  providers: [
    { provide: UserService, useClass: UserServiceStub },
    { provide: UsersService, useClass: UsersServiceStub },
    { provide: CurrentUserService, useClass: CurrentUserServiceStub },
  ],
})
export class UserTestingModule {}
