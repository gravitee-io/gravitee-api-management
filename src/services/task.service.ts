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

import { PagedResult } from '../entities/pagedResult';
import { IHttpPromise } from 'angular';
import UserService from './user.service';

class TaskService {
  private URL: string;
  private Constants: any;
  private UserService: any;

  constructor(private $http: ng.IHttpService, Constants, UserService: UserService) {
    'ngInject';
    this.Constants = Constants;
    this.URL = this.Constants.orgBaseURL + '/user/tasks';
    this.UserService = UserService;
  }

  getTaskSchedulerInSeconds(): number {
    if (this.Constants.scheduler && this.Constants.scheduler.tasks) {
      return this.Constants.scheduler.tasks;
    }
    return 10;
  }

  getTasks(): IHttpPromise<any> {
    const config = { ignoreLoadingBar: true, silentCall: true } as ng.IRequestShortcutConfig;
    return this.$http.get(this.URL, config);
  }

  fillUserTasks(tasks: PagedResult) {
    return this.UserService.setTasks(tasks);
  }
}

export default TaskService;
