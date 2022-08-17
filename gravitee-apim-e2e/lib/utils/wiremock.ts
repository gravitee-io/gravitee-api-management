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
import fetchApi from 'node-fetch';

export async function setWiremockState(scenarioName: string, state: string) {
  const { status } = await fetchApi(`${process.env.WIREMOCK_BASE_URL}/__admin/scenarios/${scenarioName}/state`, {
    method: 'PUT',
    body: JSON.stringify({ state }),
    headers: {
      'Content-Type': 'application/json',
    },
  });

  if (status !== 200) {
    throw new Error(`Unable to put scenario ${scenarioName} in state ${state} (HTTP ${status})`);
  }
}
