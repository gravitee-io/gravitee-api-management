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
import TagService from '../../../services/tag.service';
import NotificationService from '../../../services/notification.service';
import EntrypointService from "../../../services/entrypoint.service";
import PortalConfigService from "../../../services/portalConfig.service";
import {IScope} from 'angular';

class TagsController {
  private tags: any;

  constructor(
    private TagService: TagService,
    private NotificationService: NotificationService,
    private $q: ng.IQService,
    private $mdEditDialog,
    private $mdDialog: angular.material.IDialogService,
    private EntrypointService: EntrypointService,
    private Constants,
    private PortalConfigService: PortalConfigService,
    private $rootScope: IScope) {
    'ngInject';
    this.$rootScope = $rootScope;
  }

  deleteTag(tag) {
    var that = this;
    this.$mdDialog.show({
      controller: 'DeleteTagDialogController',
      template: require('./delete.tag.dialog.html'),
      locals: {
        tag: tag
      }
    }).then((deleteTag) => {
      if (deleteTag) {
        if (tag.id) {
          that.TagService.delete(tag).then(() => {
            this.deleteEntrypointsByTag(tag).then(() => {
              that.NotificationService.show("Tag '" + tag.name + "' deleted with success");
                _.remove(that.tags, tag);
              });
            });
        } else {
          _.remove(that.tags, tag);
        }
      }
    });
  }

  onClipboardSuccess(e) {
    this.NotificationService.show('Sharding Tag ID has been copied to clipboard');
    e.clearSelection();
  }

  deleteEntrypoint(entrypoint) {
    this.$mdDialog.show({
      controller: 'DeleteEntrypointDialogController',
      template: require('./entrypoint/delete.entrypoint.dialog.html'),
      locals: {
        entrypoint: entrypoint
      }
    }).then((entrypointToDelete) => {
      if (entrypointToDelete) {
        if (entrypointToDelete.id) {
          this.EntrypointService.delete(entrypointToDelete).then(() => {
            this.NotificationService.show("Entrypoint '" + entrypointToDelete.value + "' deleted with success");
            _.remove(this.entrypoints, entrypointToDelete);
          });
        }
      }
    });
  }

  deleteEntrypointsByTag(tag) {
    let promises = [];
    _.forEach(this.entrypoints, (entrypoint) => {
      if (_.includes(entrypoint.tags, tag.id)) {
        promises.push(this.EntrypointService.delete(entrypoint).then(() => {
          _.remove(this.entrypoints, entrypoint);
        }));
      }
    });
    return this.$q.all(promises);
  }

  saveSettings = () => {
    PortalConfigService.save().then( () => {
      NotificationService.show("Configuration saved!");
      this.formSettings.$setPristine();
    });
  };

  resetSettings = () => {
    PortalConfigService.get().then((response) => {
      this.Constants = response.data;
      this.formSettings.$setPristine();
    });
  };

  groupNames = (groups) => {
    // _.join(array, [separator=','])
    return _.join(_.map(groups, (groupId) => {
      return _.find(this.groups, {id: groupId}).name;
    }), ', ');
  }
}

export default TagsController;
