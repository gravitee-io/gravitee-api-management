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
class ApplicationController {
  constructor(resolvedApplication, $state, $scope, $cookieStore) {
		'ngInject';
		this.application = resolvedApplication.data;
		this.$state = $state;
		this.$scope = $scope;
		this.selectTab();
	}

	isOwner() {
    return this.application.permission && (this.application.permission === 'owner' || this.application.permission === 'primary_owner');
  }
	
	selectTab() {
	  if (this.$state.current.name.endsWith('general')) {
      this.$scope.selectedTab = 0;
    } else if (this.$state.current.name.endsWith('apikeys')) {
      this.$scope.selectedTab = 1;
    } else if (this.$state.current.name.endsWith('members')) {
      this.$scope.selectedTab = 2;
    }
	}
}

export default ApplicationController;
