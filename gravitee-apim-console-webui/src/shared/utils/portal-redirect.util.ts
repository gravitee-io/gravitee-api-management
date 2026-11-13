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
import { Constants } from '../../entities/Constants';

/**
 * Builds the Management API URL that redirects to the Developer Portal.
 * `version=next` is only added while the Classic portal is the default one: once the New Developer Portal
 * is the default, it is served at the portal root and `/next` no longer exists.
 */
export const toPortalRedirectUrl = (constants: Constants, openNewPortal: boolean): string => {
  const redirectUrl = constants.env.baseURL.replace('{:envId}', constants.org.currentEnv.id) + '/portal/redirect';
  const isClassicPortalDefault = !constants.defaultPortal || constants.defaultPortal === 'classic';
  return openNewPortal && isClassicPortalDefault ? `${redirectUrl}?version=next` : redirectUrl;
};
