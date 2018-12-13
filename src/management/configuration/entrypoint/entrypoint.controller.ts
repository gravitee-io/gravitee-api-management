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
import NotificationService from '../../../services/notification.service';
import EntrypointService from "../../../services/entrypoint.service";

class EntrypointController {
  private entrypoint: any;

  constructor(private EntrypointService: EntrypointService,
              private $state,
              private NotificationService: NotificationService) {
    'ngInject';
    if ($state.params.entrypointId) {
      EntrypointService.findById($state.params.entrypointId).then((response) => {
        this.entrypoint = response.data;
      });
    } else {
      this.entrypoint = {tags: []};
    }
  }

  save(entrypoint) {
    if (entrypoint.id) {
      this.EntrypointService.update(entrypoint).then(() => {
        this.NotificationService.show("Entrypoint updated with success");
        this.$state.go('management.settings.tags');
      });
    } else {
      this.EntrypointService.create(entrypoint).then(() => {
        this.NotificationService.show("Entrypoint created with success");
        this.$state.go('management.settings.tags');
      });
    }
  }
}

export default EntrypointController;
