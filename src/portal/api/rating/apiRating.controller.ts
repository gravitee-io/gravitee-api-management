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
import ApiService from '../../../services/api.service';
import NotificationService from '../../../services/notification.service';
import _ = require('lodash');

class ApiPortalRatingController {
  private api: any;
  private formRating: any;
  private pager: any;
  private ratings;
  private rating;
  private ratingAnswer;

  constructor(private NotificationService: NotificationService,
              private ApiService: ApiService,
              private $scope,
              private $mdDialog,
              private $state,
              private Constants,
              private UserService) {
    'ngInject';

    if (!(Constants.rating && Constants.rating.enabled)) {
      $state.go('portal.home');
    }
  }

  $onInit() {
    if (!this.rating) {
      this.rating = {rate: 0}
    }

    let pageNumbers = _.range(1, _.ceil(this.ratings.totalElements / 10) + 1);
    this.pager = {
      pages: pageNumbers,
      currentPage: this.ratings.pageNumber
    };
  }

  setPage(pageNumber) {
    this.$state.go(this.$state.current, {pageNumber: pageNumber});
  }

  save() {
    if (this.rating.id) {
      this.ApiService.updateRating(this.api.id, this.rating).then(() => {
        this.onModification();
        this.NotificationService.show('api.rating.successUpdate');
      });
    } else {
      this.ApiService.createRating(this.api.id, this.rating).then(() => {
        this.onModification();
        this.NotificationService.show('api.rating.successCreation');
      }, () => delete this.rating);
    }
    this.formRating.$setPristine();
  }

  saveRate() {
    // do not save if modifications on form, wait for submission
    if (this.formRating.$pristine) {
      this.save();
    }
  }

  onModification() {
    this.setPage(1);
    this.ApiService.getApiRatingForConnectedUser(this.api.id).then((response) => {
      this.rating = response.data;
      this.$onInit();
    });
    this.ApiService.getApiRatings(this.api.id, this.$state.params['pageNumber']).then((response) => {
      this.ratings = response.data;
      this.$onInit();
    });
    this.$scope.$emit('onRatingSave');
  }

  displayCommentPart() {
    return (this.rating && this.rating.id && !this.rating.title) || this.formRating.$dirty;
  }

  createAnswer(ratingId) {
    this.ApiService.createRatingAnswer(this.api.id, ratingId, this.ratingAnswer).then(() => {
      delete this.ratingAnswer;
      this.onModification();
      this.NotificationService.show('api.rating.answer.successCreation');
    });
  }

  delete(ratingId, answer) {
    this.$mdDialog.show({
      controller: function ($scope, $mdDialog, answer) {
        'ngInject';
        $scope.answer = answer;

        this.cancel = function () {
          $mdDialog.cancel();
        };
        this.ok = function () {
          $mdDialog.hide();
        };
      },
      locals: {
        answer: answer
      },
      controllerAs: '$ctrl',
      template: '<md-dialog aria-label="delete-rating">' +
        '<md-dialog-content layout-padding>' +
          '<h4 translate="{{answer?\'api.rating.answer.deletion\':\'api.rating.deletion\'}}"></h4>' +
        '</md-dialog-content>' +
        '<md-dialog-actions layout="row">' +
          '<md-button ng-click="$ctrl.cancel()" class="md-primary">' +
            '{{\'common.cancel\' | translate}}' +
          '</md-button>' +
          '<md-button ng-click="$ctrl.ok()" class="md-raised md-primary">' +
            '{{\'common.ok\' | translate}}' +
          '</md-button>' +
        '</md-dialog-actions>' +
      '</md-dialog>'
    }).then(() => {
      if (answer) {
        this.ApiService.deleteRatingAnswer(this.api.id, ratingId, answer).then(() => {
          this.onModification();
          this.NotificationService.show('api.rating.answer.successDeletion');
        });
      } else {
        this.ApiService.deleteRating(this.api.id, ratingId).then(() => {
          this.onModification();
          this.NotificationService.show('api.rating.successDeletion');
        });
      }
    }, () => {
      // dialog has been canceled
    });
  }

  isUserHasPermissions(permission) {
    return this.UserService.isUserHasPermissions([permission]);
  }
}

export default ApiPortalRatingController;
