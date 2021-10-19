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
import { IScope } from 'angular';

import NotificationService from '../../services/notification.service';
import UserService from '../../services/user.service';

const NewsletterReminderComponent: ng.IComponentOptions = {
  template: require('./newsletter-reminder.html'),
  bindings: {},
  controller: function ($element, Constants: any, UserService: UserService, $scope: IScope, NotificationService: NotificationService) {
    'ngInject';
    this.displayNewsletterSubscription = false;
    this.user = null;
    this.error = false;

    $scope.$on('graviteeUserRefresh', (event, { user, refresh }) => {
      if (refresh) {
        this._loadUser();
      } else if (user && user.authenticated) {
        this.setUser(user);
      } else {
        this.setUser(null);
      }
    });

    this._loadUser = () => {
      UserService.current()
        .then((user) => {
          this.setUser(user);
        })
        .catch(() => this.setUser(null));
    };

    this.$onInit = () => {
      this._loadUser();
    };

    this.setUser = (user) => {
      if (user != null) {
        this.displayNewsletterSubscription = user.displayNewsletterSubscription;
        if (this.displayNewsletterSubscription) {
          $element.addClass('newsletter-open');
          this.user = user;
          this.email = this.user.email;
        }
      } else {
        $element.removeClass('newsletter-open');
        this.user = null;
        this.email = null;
      }
    };

    this.close = () => {
      $element.removeClass('newsletter-open');
    };

    this.subscribe = () => {
      if (this.email != null && this.email.trim() !== '') {
        UserService.subscribeNewsletter(this.email).then((user) => {
          this.setUser(user);
          NotificationService.show('Your newsletter preference has been saved.');
        });
      } else {
        this.error = true;
      }
    };

    this.unsubscribe = () => {
      this.user.newsletter = false;
      UserService.save(this.user).then((user) => {
        this.setUser(user);
        NotificationService.show('Your newsletter preference has been saved.');
      });
    };
  },
};

export default NewsletterReminderComponent;
