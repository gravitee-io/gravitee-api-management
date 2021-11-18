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
import NotificationService from '../../../../services/notification.service';
import TagService from '../../../../services/tag.service';
class TagController {
  private tag: any;

  constructor(private TagService: TagService, private $state, private NotificationService: NotificationService) {
    'ngInject';
  }

  $onInit = () => {
    if (this.$state.params.tagId === 'new') {
      this.tag = {};
    } else {
      this.TagService.get(this.$state.params.tagId).then((response) => {
        this.tag = response.data;
      });
    }
  };

  save(tag) {
    if (tag.id) {
      this.TagService.update(tag).then(() => {
        this.NotificationService.show('Tag updated with success');
        this.$state.go('organization.settings.ajs-tags');
      });
    } else {
      this.TagService.create(tag).then(() => {
        this.NotificationService.show('Tag created with success');
        this.$state.go('organization.settings.ajs-tags');
      });
    }
  }
}

export default TagController;
