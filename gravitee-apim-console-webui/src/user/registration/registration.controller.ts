import NotificationService from '../../services/notification.service';
import ReCaptchaService from '../../services/reCaptcha.service';
import UserService from '../../services/user.service';
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
class RegistrationController {
  user: { firstname?: string; lastname?: string; email?: string; customFields?: any } = {};
  fields: any[] = [];

  constructor(
    private UserService: UserService,
    private $scope,
    private NotificationService: NotificationService,
    private ReCaptchaService: ReCaptchaService,
  ) {
    'ngInject';
    this.UserService = UserService;
    this.$scope = $scope;
    this.NotificationService = NotificationService;
    this.ReCaptchaService = ReCaptchaService;
  }

  $onInit() {
    this.ReCaptchaService.displayBadge();
    this.UserService.customUserFieldsToRegister().then((resp) => (this.fields = resp.data));
  }

  register() {
    const scope = this.$scope;
    const notificationService = this.NotificationService;

    this.ReCaptchaService.execute('register')
      .then(() => this.UserService.register(this.user))
      .then(
        () => {
          scope.formRegistration.$setPristine();
          notificationService.show('Thank you for registering, you will receive an e-mail confirmation in few minutes');
        },
        (e) => {
          notificationService.showError(e);
        },
      );
  }
}

export default RegistrationController;
