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
class NotificationService {
  constructor(private $mdToast: ng.material.IToastService) {}

  show(message: any, errorStatus?: number) {
    const msg = message.statusText || message;
    const preconditionFailed = errorStatus === 412;
    this.$mdToast
      .show(
        this.$mdToast
          .simple()
          .action(preconditionFailed ? 'Refresh' : '')
          .textContent(preconditionFailed ? 'The API version is outdated and must be refreshed (current modifications will be lost)' : msg)
          .position('bottom right')
          .hideDelay(preconditionFailed ? 10000 : 3000)
          .theme(errorStatus ? 'toast-error' : 'toast-success'),
      )
      .then(response => {
        if (response === 'ok') {
          window.location.reload();
        }
      });
  }

  showError(error: any, message?: string) {
    this.show(
      message ||
        (error.data
          ? Array.isArray(error.data)
            ? error.data[0].message
            : error.data.message || (typeof error.data === 'string' ? error.data : error.statusText)
          : error),
      error.status || true,
    );
  }
}
NotificationService.$inject = ['$mdToast'];

export default NotificationService;
