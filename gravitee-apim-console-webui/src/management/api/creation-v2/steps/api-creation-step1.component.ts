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
import ApiCreationV2ControllerAjs from './api-creation-v2.controller.ajs';
import { shouldDisplayHint } from './form.helper';

import ApiPrimaryOwnerModeService from '../../../../services/apiPrimaryOwnerMode.service';

const ApiCreationStep1Component: ng.IComponentOptions = {
  require: {
    parent: '^apiCreationV2ComponentAjs',
  },
  template: require('html-loader!./api-creation-step1.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
  controller: [
    'ApiPrimaryOwnerModeService',
    class {
      private parent: ApiCreationV2ControllerAjs;
      private advancedMode: boolean;
      public shouldDisplayHint = shouldDisplayHint;
      private scrollContainer: HTMLElement | null = null;
      private scrollListener: (() => void) | null = null;
      private isLoading = false; // to prevent repititive call at once
      private hasLoadedOnce = false;
      public hasMoreGroups = true;
      public useGroupAsPrimaryOwner = false;

      constructor(private ApiPrimaryOwnerModeService: ApiPrimaryOwnerModeService) {
        this.advancedMode = false;
      }

      $onInit() {
        if (this.ApiPrimaryOwnerModeService.isGroupOnly()) {
          this.useGroupAsPrimaryOwner = true; // auto-enable select for group-only mode
        }
      }

      toggleAdvancedMode = () => {
        this.advancedMode = !this.advancedMode;
        if (!this.advancedMode) {
          this.parent.api.groups = [];
        }
      };

      canUseAdvancedMode = () => {
        return (
          (this.ApiPrimaryOwnerModeService.isHybrid() &&
            ((this.parent.attachableGroups && this.parent.attachableGroups.length > 0) ||
              (this.parent.poGroups && this.parent.poGroups.length > 0))) ||
          (this.ApiPrimaryOwnerModeService.isGroupOnly() && this.parent.attachableGroups && this.parent.attachableGroups.length > 0)
        );
      };

      onSelectOpen = () => {
        // Wait for DOM to render
        setTimeout(() => {
          this.scrollContainer = document.querySelector('[role="listbox"][aria-label="Groups"]');
          const boundScrollHandler = this.onScroll.bind(this);
          // Add scroll event listener
          this.scrollContainer.addEventListener('scroll', boundScrollHandler);
          // Cleaning: Store the scroll event removal function
          this.scrollListener = () => {
            this.scrollContainer?.removeEventListener('scroll', boundScrollHandler);
          };
        }, 100);
      };

      onScroll(event: Event): void {
        const percentScroll = 0.7;
        const target = event.target as HTMLElement;
        const scrollThreshold = percentScroll * target.scrollHeight;

        if (target.scrollTop + target.clientHeight >= scrollThreshold && this.parent.hasMoreGroups && !this.isLoading) {
          // First request: No delay
          if (!this.hasLoadedOnce) {
            this.parent.loadMoreGroups();
            this.hasLoadedOnce = true;
          }

          // For the other requests: 1 second delay
          else {
            this.isLoading = true;
            // Release lock after 1 second
            setTimeout(() => {
              this.parent.loadMoreGroups();
              this.isLoading = false;
            }, 1000);
          }
        }
      }

      onSelectClose = () => {
        this.cleanupScrollListener();
      };

      private cleanupScrollListener = () => {
        if (this.scrollListener) {
          this.scrollListener();
          this.scrollListener = null;
        }
        this.scrollContainer = null;
      };

      // Don't forget to clean up when component is destroyed
      $onDestroy() {
        this.cleanupScrollListener();
      }
    },
  ],
};

export default ApiCreationStep1Component;
