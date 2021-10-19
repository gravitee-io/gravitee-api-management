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

import { IHttpPromise } from 'angular';

import UserService from './user.service';

import { Constants } from '../entities/Constants';
import { PagedResult } from '../entities/pagedResult';
import { User } from '../entities/user';

class TaskService {
  constructor(private readonly $http: ng.IHttpService, private readonly Constants: Constants, private readonly UserService: UserService) {
    'ngInject';
  }

  getTaskSchedulerInSeconds(): number {
    if (this.Constants.org.settings.scheduler && this.Constants.org.settings.scheduler.tasks) {
      return this.Constants.org.settings.scheduler.tasks;
    }
    return 10;
  }

  getTasks(): IHttpPromise<any> {
    const config = { ignoreLoadingBar: true, silentCall: true } as ng.IRequestShortcutConfig;
    return this.$http.get(this.Constants.org.baseURL + '/user/tasks', config);
  }

  fillUserTasks(tasks: PagedResult): User {
    return this.UserService.setTasks(tasks);
  }
}

export default TaskService;
