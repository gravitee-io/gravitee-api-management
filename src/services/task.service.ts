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

import {User} from "../entities/user";
import {PagedResult} from "../entities/pagedResult";

class TaskService {
  private URL: string;
  private Constants: any;

  constructor(private $http: ng.IHttpService, Constants) {
    'ngInject';
    this.Constants = Constants;
    this.URL = this.Constants.baseURL+"user/tasks";
  }

  getTaskSchedulerInSeconds() {
    if (this.Constants.scheduler && this.Constants.scheduler.tasks) {
      return this.Constants.scheduler.tasks;
    }
    return 10;
  }

  getTasks() {
    const config = { ignoreLoadingBar: true, silentCall: true } as ng.IRequestShortcutConfig;
    return this.$http.get(this.URL, config);
  }

  fillUserTasks(user: User, tasks: PagedResult) {
    user.tasks.metadata = tasks.metadata;
    user.tasks.data = tasks.data;
    user.tasks.page = tasks.page;
  }
}

export default TaskService;
