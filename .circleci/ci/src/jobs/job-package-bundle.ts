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
import { commands, Config, Job, reusable } from '@circleci/circleci-config-sdk';
import { BaseExecutor } from '../executors';
import { parse } from '../utils';
import { SyncFolderToS3Command } from '../commands';

export class PackageBundleJob {
  public static create(dynamicConfig: Config, graviteeioVersion: string, isDryRun: boolean) {
    const parsedGraviteeioVersion = parse(graviteeioVersion);

    const syncFolderToS3Cmd = SyncFolderToS3Command.get(dynamicConfig, parsedGraviteeioVersion, isDryRun);
    dynamicConfig.addReusableCommand(syncFolderToS3Cmd);

    const graviteeFullDistrib = `graviteeio-full-${graviteeioVersion}`;
    const publishFolderPath = `graviteeio-apim/distributions`;
    const zipName = `${graviteeFullDistrib}.zip`;
    const fullDistributionDir = `./folder_to_sync/${publishFolderPath}/${graviteeFullDistrib}`;

    return new Job('job-package-bundle', BaseExecutor.create('small'), [
      new commands.workspace.Attach({ at: '.' }),
      new commands.Run({
        name: 'Building full-distribution bundle',
        command: `mkdir -p ${fullDistributionDir}
# Console
cp -r gravitee-apim-console-webui/dist ${fullDistributionDir}/graviteeio-apim-console-ui-${graviteeioVersion}

# Portal
cp -r gravitee-apim-portal-webui/dist ${fullDistributionDir}/graviteeio-apim-portal-ui-${graviteeioVersion}

# Rest API
cp -r gravitee-apim-rest-api/gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target/distribution ${fullDistributionDir}/graviteeio-apim-rest-api-${graviteeioVersion}

# Gateway
cp -r gravitee-apim-gateway/gravitee-apim-gateway-standalone/gravitee-apim-gateway-standalone-distribution/target/distribution ${fullDistributionDir}/graviteeio-apim-gateway-${graviteeioVersion}

cd ./folder_to_sync/${publishFolderPath}
zip -q -r ${zipName} ${graviteeFullDistrib}

md5sum ${zipName} > ${zipName}.md5
sha512sum ${zipName} > ${zipName}.sha512sum
sha1sum ${zipName} > ${zipName}.sha1

rm -rf ${graviteeFullDistrib}
`,
      }),
      new reusable.ReusedCommand(syncFolderToS3Cmd, {
        'folder-to-sync': 'folder_to_sync',
      }),
    ]);
  }
}
