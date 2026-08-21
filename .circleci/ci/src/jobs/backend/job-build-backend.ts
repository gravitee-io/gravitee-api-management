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
import { Command, Config, Job, commands, reusable } from '../../circleci-config';
import { OpenJdkNodeExecutor } from '../../executors';
import { InstallYarnCommand, NotifyOnFailureCommand, RestoreMavenJobCacheCommand, SaveMavenJobCacheCommand } from '../../commands';
import { config } from '../../config';
import { CircleCIEnvironment } from '../../pipelines';
import { mavenParallelism } from '../../utils';

export class BuildBackendJob {
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    const jobName = 'job-build';

    const restoreMavenJobCacheCmd = RestoreMavenJobCacheCommand.get(environment);
    const saveMavenJobCacheCmd = SaveMavenJobCacheCommand.get();
    const notifyOnFailureCmd = NotifyOnFailureCommand.get(dynamicConfig, environment);
    // The reactor installs the yarn workspace (gravitee-gamma runs `yarn install` in
    // generate-resources). Without corepack the image's yarn 1 cannot read the berry lockfile
    // and resolves the whole workspace from the registry instead.
    const installYarnCmd = InstallYarnCommand.get();
    dynamicConfig.addReusableCommand(restoreMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(saveMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(notifyOnFailureCmd);
    dynamicConfig.addReusableCommand(installYarnCmd);

    const steps: Command[] = [
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new reusable.ReusedCommand(restoreMavenJobCacheCmd, { jobName: jobName }),
      new reusable.ReusedCommand(installYarnCmd),
      new commands.Run({
        name: 'Build engine',
        command: `mvn -s ${config.maven.settingsFile} clean install --no-transfer-progress --update-snapshots -DskipTests -Dskip.validation=true -Dgravitee.archrules.skip=false ${mavenParallelism('large')} -P all-modules -DwithJavadoc`,
        environment: {
          BUILD_ID: environment.buildId,
          BUILD_NUMBER: environment.buildNum,
          GIT_COMMIT: environment.sha1,
          // Cap the maven JVM heap: its default is derived from the memory of the
          // underlying CI host, not from the resource class of the job.
          MAVEN_OPTS: '-Xmx2048m',
        },
      }),
      new commands.Run({
        // Second phase: assemble against the engine just installed above, not the released one.
        // -nsu so a published snapshot cannot take its place.
        name: 'Build distribution',
        command: `mvn -s ${config.maven.settingsFile} -f gravitee-apim-distribution/pom.xml clean install --no-transfer-progress -nsu -DskipTests -Dskip.validation=true -Dgravitee.archrules.skip=false ${mavenParallelism('large')} -Dbundle=dev -Pengine-snapshot,integration-tests-modules -DwithJavadoc`,
        environment: {
          MAVEN_OPTS: '-Xmx2048m',
        },
      }),
      new reusable.ReusedCommand(notifyOnFailureCmd),
      new commands.cache.Save({
        paths: ['~/.m2/repository/io/gravitee/apim', '~/.m2/repository/io/gravitee/gamma'],
        key: `${config.cache.prefix}-build-apim-{{ .Environment.CIRCLE_WORKFLOW_WORKSPACE_ID }}`,
        when: 'on_success',
      }),
      new commands.Run({
        // The rest-api test job runs surefire against these instead of compiling and generating
        // again. -DskipTests above compiles the tests without running them, so test-classes are
        // already here. Two levels of glob because the modules nest one deep in places.
        //
        // A cache rather than the workspace: every job attaches the workspace whole, so putting a
        // few hundred megabytes of classes there would bill all 30-odd of them for something only
        // this one consumer reads. save_cache takes literal paths, hence the archive.
        name: 'Archive the compiled rest-api classes',
        command: `tar -cf ${config.cache.restApiClassesArchive} ./gravitee-apim-rest-api/*/target/classes ./gravitee-apim-rest-api/*/target/test-classes ./gravitee-apim-rest-api/*/*/target/classes ./gravitee-apim-rest-api/*/*/target/test-classes`,
      }),
      new commands.cache.Save({
        paths: [config.cache.restApiClassesArchive],
        key: `${config.cache.prefix}-rest-api-classes-{{ .Environment.CIRCLE_WORKFLOW_WORKSPACE_ID }}`,
        when: 'on_success',
      }),
      new reusable.ReusedCommand(saveMavenJobCacheCmd, { jobName: jobName }),
      new commands.workspace.Persist({
        root: './',
        paths: [
          './gravitee-apim-rest-api/gravitee-apim-rest-api-management/gravitee-apim-rest-api-management-rest/target/classes/console-openapi.*',
          './gravitee-apim-distribution/gravitee-apim-distribution-standalone/gravitee-apim-distribution-standalone-rest-api/target/distribution',
          './gravitee-apim-distribution/gravitee-apim-distribution-standalone/gravitee-apim-distribution-standalone-gateway/target/distribution',
        ],
      }),
    ];
    return new Job(jobName, OpenJdkNodeExecutor.create('large'), steps);
  }
}
