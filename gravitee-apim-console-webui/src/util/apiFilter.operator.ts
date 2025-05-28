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
import { switchMap } from 'rxjs/operators';
import { EMPTY, of } from 'rxjs';

import { Api } from '../entities/management-api-v2';
import { SnackBarService } from '../services-ngx/snack-bar.service';

/**
 * Filter API V1 and V4 to define
 * - V4 as not supported
 * - V1 as deprecated
 */
export const onlyApiV2Filter = (snackBarService?: SnackBarService) =>
  switchMap((api: Api) => {
    if (api.definitionVersion === 'V4') {
      if (snackBarService) {
        snackBarService.error('API V4 not supported.');
        return EMPTY;
      }
      throw new Error('API V4 not supported.');
    }

    if (api.definitionVersion === 'FEDERATED') {
      if (snackBarService) {
        snackBarService.error('API FEDERATED not supported.');
        return EMPTY;
      }
      throw new Error('API FEDERATED not supported.');
    }

    if (api.definitionVersion === 'FEDERATED_AGENT') {
      if (snackBarService) {
        snackBarService.error('API FEDERATED_AGENT not supported.');
        return EMPTY;
      }
      throw new Error('API FEDERATED_AGENT not supported.');
    }

    if (api.definitionVersion === 'V1') {
      if (snackBarService) {
        snackBarService.error('API V1 are deprecated. Please upgrade your API to V2.');
        return EMPTY;
      }
      throw new Error('API V1 are deprecated. Please upgrade your API to V2.');
    }
    return of(api);
  });

/**
 * Filter API V4 to define V4 as not supported
 */
export const onlyApiV1V2Filter = (snackBarService?: SnackBarService) =>
  switchMap((api: Api) => {
    if (api.definitionVersion === 'V4') {
      if (snackBarService) {
        snackBarService.error('API V4 not supported.');
        return EMPTY;
      }
      throw new Error('API V4 not supported.');
    }

    if (api.definitionVersion === 'FEDERATED') {
      if (snackBarService) {
        snackBarService.error('API FEDERATED not supported.');
        return EMPTY;
      }
      throw new Error('API FEDERATED not supported.');
    }

    if (api.definitionVersion === 'FEDERATED_AGENT') {
      if (snackBarService) {
        snackBarService.error('API FEDERATED_AGENT not supported.');
        return EMPTY;
      }
      throw new Error('API FEDERATED_AGENT not supported.');
    }

    return of(api);
  });

/**
 * Filter API V4 only
 */
export const onlyApiV4Filter = (snackBarService?: SnackBarService) =>
  switchMap((api: Api) => {
    if (api.definitionVersion !== 'V4') {
      if (snackBarService) {
        snackBarService.error('Only V4 APIs are supported.');
        return EMPTY;
      }
      throw new Error('Only V4 APIs are supported.');
    }

    return of(api);
  });

export const onlyApiV2V4Filter = (snackBarService?: SnackBarService) =>
  switchMap((api: Api) => {
    if (api.definitionVersion === 'V1') {
      if (snackBarService) {
        snackBarService.error('API V1 not supported.');
        return EMPTY;
      }
      throw new Error('API V1 not supported.');
    }

    if (api.definitionVersion === 'FEDERATED') {
      if (snackBarService) {
        snackBarService.error('API FEDERATED not supported.');
        return EMPTY;
      }
      throw new Error('API FEDERATED not supported.');
    }

    return of(api);
  });
