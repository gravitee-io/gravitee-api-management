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
import UserService from '../services/user.service';

class LoginController {
  user = {};
  userCreationEnabled: boolean;

    constructor(private UserService: UserService, private $state: ng.ui.IStateService, Constants) {
    'ngInject';
    this.userCreationEnabled = Constants.userCreationEnabled;
  }

	login($event: Event) {
	  $event.preventDefault();
    this.UserService.login(this.user).then(() => {
      this.$state.go('home', {}, {reload: true, inherit: false});
    });
	}
}

export default LoginController;
