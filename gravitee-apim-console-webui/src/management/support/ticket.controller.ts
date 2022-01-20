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
import * as _ from 'lodash';

import { ApiService } from '../../services/api.service';
import ApplicationService from '../../services/application.service';
import NotificationService from '../../services/notification.service';
import TicketService from '../../services/ticket.service';
import UserService from '../../services/user.service';

class SupportTicketController {
  private ticket: any;
  private apis: [any];
  private applications: [any];
  private isAuthenticated: boolean;
  private userHasAnEmail: boolean;
  private stateParams: any;
  private formTicket: any;

  constructor(
    private TicketService: TicketService,
    private NotificationService: NotificationService,
    private UserService: UserService,
    private ApiService: ApiService,
    private ApplicationService: ApplicationService,
    private $stateParams,
  ) {
    'ngInject';

    this.stateParams = $stateParams;

    if ((this.isAuthenticated = UserService.isAuthenticated())) {
      this.userHasAnEmail = !!UserService.currentUser.email;
      ApiService.list()
        .then((response) => (this.apis = response.data))
        .then((apis) => {
          if ($stateParams.apiId) {
            const api = _.find(apis, { id: $stateParams.apiId });
            if (api) {
              this.ticket = {
                api: $stateParams.apiId,
              };
            }
          }
        });
      ApplicationService.list().then((response) => (this.applications = response.data));
    }
  }

  create() {
    this.TicketService.create(this.ticket).then(() => {
      this.NotificationService.show('Support ticket has been created successfully');
      this.formTicket.$setPristine();
      this.formTicket.$setUntouched();
      this.ticket = {};
    });
  }
}

export default SupportTicketController;
