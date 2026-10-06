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
      new commands.Run({
        // Same layout as the components published up to 4.7: <folder>/<name>-<version>.zip, whose
        // single root directory is <name>-<version>, next to its checksums.
        // The gateway and REST API zips keep only the plugins their own module ships, as before 4.8:
        // the bundle-default plugins stay in the full bundle. The folder ext/ is kept, emptied.
        name: 'Packaging components',
        command: `componentsDir=$(pwd)/folder_to_sync/graviteeio-apim/components
staging=$(mktemp -d)

package_component() {
  source=$1
  folder=$2
  name=$3
  destination=$componentsDir/$folder
  mkdir -p $destination

  cp -r $source $staging/$name-${graviteeioVersion}
  if [ -n "$4" ]; then
    find $staging/$name-${graviteeioVersion}/plugins -mindepth 1 -maxdepth 1 ! -name "$4*" ! -name .gitignore ! -name ext -exec rm -rf {} +
    find $staging/$name-${graviteeioVersion}/plugins/ext -type f -delete
  fi
  (cd $staging && zip -q -r $destination/$name-${graviteeioVersion}.zip $name-${graviteeioVersion})
  rm -rf $staging/$name-${graviteeioVersion}

  cd $destination
  md5sum $name-${graviteeioVersion}.zip > $name-${graviteeioVersion}.zip.md5
  sha512sum $name-${graviteeioVersion}.zip > $name-${graviteeioVersion}.zip.sha512sum
  sha1sum $name-${graviteeioVersion}.zip > $name-${graviteeioVersion}.zip.sha1
  cd - > /dev/null
}

package_component gravitee-apim-console-webui/dist gravitee-management-webui gravitee-apim-console-webui
package_component gravitee-apim-portal-webui/dist gravitee-portal-webui gravitee-apim-portal-webui
package_component gravitee-apim-rest-api/gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target/distribution gravitee-management-rest-api gravitee-apim-rest-api gravitee-apim-rest-api-
package_component gravitee-apim-gateway/gravitee-apim-gateway-standalone/gravitee-apim-gateway-standalone-distribution/target/distribution gravitee-gateway gravitee-apim-gateway gravitee-apim-gateway-
`,
      }),
      new reusable.ReusedCommand(syncFolderToS3Cmd, {
        'folder-to-sync': 'folder_to_sync',
      }),
    ]);
  }
}
