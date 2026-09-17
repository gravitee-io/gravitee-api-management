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
import { AzureArtifactsTokenCommand, NotifyOnFailureCommand, RestoreMavenJobCacheCommand, SaveMavenJobCacheCommand } from '../../commands';
import { config } from '../../config';
import { CircleCIEnvironment } from '../../pipelines';
import { computeApimVersion, mavenParallelism } from '../../utils';

/**
 * The distribution assembled against the engine BuildEngineJob installed, as a job of its own:
 * the `Build distribution` step of BuildBackendJob, for the branches of config.snapshotBranches.
 *
 * It resolves the plugins the distribution bundles from the snapshot repositories, and some of
 * them are built against this very engine, so it can only pass once the engine has been published
 * and they have been rebuilt on it. Its failure therefore gates the workflow's status, not the
 * engine's publication.
 */
export class BuildDistributionJob {
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    const jobName = 'job-build-distribution';

    const restoreMavenJobCacheCmd = RestoreMavenJobCacheCommand.get(environment);
    const saveMavenJobCacheCmd = SaveMavenJobCacheCommand.get();
    const notifyOnFailureCmd = NotifyOnFailureCommand.get(dynamicConfig, environment);
    const azureArtifactsTokenCmd = AzureArtifactsTokenCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(restoreMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(saveMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(notifyOnFailureCmd);
    dynamicConfig.addReusableCommand(azureArtifactsTokenCmd);

    const steps: Command[] = [
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new reusable.ReusedCommand(restoreMavenJobCacheCmd, { jobName: jobName }),
      // The engine this workflow built, from where BuildEngineJob left it.
      new commands.cache.Restore({
        keys: [`${config.cache.prefix}-build-apim-{{ .Environment.CIRCLE_WORKFLOW_WORKSPACE_ID }}`],
      }),
      new reusable.ReusedCommand(azureArtifactsTokenCmd),
      new commands.Run({
        // Assembled against the engine restored above, not a published one: -nsu so a snapshot
        // cannot take its place, and the version passed explicitly, as the Taskfile does.
        name: 'Build distribution',
        command: `mvn -s ${config.maven.settingsFile} -f gravitee-apim-distribution/pom.xml clean install --no-transfer-progress -nsu -DskipTests -Dskip.validation=true -Dgravitee.archrules.skip=false ${mavenParallelism('large')} -Dbundle=dev -Pintegration-tests-modules -Dapim.core.version=${computeApimVersion(environment)} -DwithJavadoc`,
        environment: {
          MAVEN_OPTS: '-Xmx2048m',
        },
      }),
      new reusable.ReusedCommand(notifyOnFailureCmd),
      new reusable.ReusedCommand(saveMavenJobCacheCmd, { jobName: jobName }),
      new commands.workspace.Persist({
        root: './',
        paths: [
          './gravitee-apim-distribution/gravitee-apim-distribution-standalone/gravitee-apim-distribution-standalone-rest-api/target/distribution',
          './gravitee-apim-distribution/gravitee-apim-distribution-standalone/gravitee-apim-distribution-standalone-gateway/target/distribution',
        ],
      }),
    ];

    return new Job(jobName, OpenJdkNodeExecutor.create('xlarge'), steps);
  }
}
