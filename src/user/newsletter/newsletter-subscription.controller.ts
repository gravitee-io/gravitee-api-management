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
import UserService from '../../services/user.service';
import '@gravitee/ui-components/wc/gv-newsletter-subscription';

class NewsletterSubscriptionController {
  newsletterPage;

  constructor(
    private $state,
    private $scope,
    public UserService: UserService,
    private NotificationService,
    Constants,
    private $window,
    private $rootScope,
    private taglines,
  ) {
    'ngInject';

    $scope.user = UserService.currentUser;

    this.newsletterPage = document.querySelector('gv-newsletter-subscription');
    this.newsletterPage.addEventListener('gv-newsletter-subscription:subscribe', this.onSubscribe.bind(this));
    this.newsletterPage.addEventListener('gv-newsletter-subscription:skip', this.onSkip.bind(this));
    this.newsletterPage.disabled = !!$scope.user.email;
    if (taglines) {
      this.newsletterPage.taglines = taglines;
    }
  }

  onSubscribe({ detail }) {
    if (detail && detail.trim() !== '') {
      this.UserService.subscribeNewsletter(detail).then(() => {
        this.NotificationService.show('Your newsletter preference has been saved.');
        this._updateState();
      });
    }
  }

  onSkip() {
    this._updateState();
  }

  _updateState() {
    this.$window.localStorage.setItem('newsletterProposed', true);
    this.$rootScope.$broadcast('graviteeUserRefresh', { user: this.$scope.user, refresh: true });
    this.$state.go('management');
  }
}

export default NewsletterSubscriptionController;
