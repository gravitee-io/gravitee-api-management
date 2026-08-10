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
import { OpenJdkExecutor } from '../../executors';
import { NotifyOnFailureCommand, RestoreMavenJobCacheCommand, SaveMavenJobCacheCommand } from '../../commands';
import { config } from '../../config';
import { CircleCIEnvironment } from '../../pipelines';
import { mavenParallelism } from '../../utils';

export class ValidateJob {
  private static jobName = 'job-validate';
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    const restoreMavenJobCacheCmd = RestoreMavenJobCacheCommand.get(environment);
    const saveMavenJobCacheCmd = SaveMavenJobCacheCommand.get();
    const notifyOnFailureCmd = NotifyOnFailureCommand.get(dynamicConfig, environment);
    dynamicConfig.addReusableCommand(restoreMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(saveMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(notifyOnFailureCmd);

    const steps: Command[] = [
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new reusable.ReusedCommand(restoreMavenJobCacheCmd, { jobName: ValidateJob.jobName }),
      new commands.Run({
        name: 'Validate project',
        command: `mvn -s ${config.maven.settingsFile} validate -Dgravitee.archrules.skip=true --no-transfer-progress -Pall-modules ${mavenParallelism('medium')}`,
      }),
      new commands.Run({
        // Its own reactor, so validated separately. No engine-snapshot here: the profile makes the
        // BOM import resolve ${revision}${sha1}${changelist}, and this step runs before anything is
        // installed. It works only for as long as that snapshot happens to be on Nexus — right
        // after a <revision> bump none exists, and validation would hard-fail on every pull request
        // until the first publication. License and prettier do not care which engine is pinned.
        name: 'Validate distribution',
        command: `mvn -s ${config.maven.settingsFile} -f gravitee-apim-distribution/pom.xml validate -nsu -Dgravitee.archrules.skip=true --no-transfer-progress -Pintegration-tests-modules ${mavenParallelism('medium')}`,
      }),
      new commands.Run({
        // The two reactors each carry a version triplet and they must stay in step: engine-snapshot
        // resolves apim.server.version from the distribution's own properties, so a stale triplet
        // does not fail — it resolves the previous version's snapshot from Nexus and quietly
        // assembles the wrong engine. Cheap to check, expensive to notice otherwise.
        name: 'Check both reactors carry the same version',
        command: `triplet() {
  rev=$(grep -o '<revision>[^<]*</revision>' "$1" | head -1 | sed -E 's#</?revision>##g')
  chg=$(grep -o '<changelist>[^<]*</changelist>' "$1" | head -1 | sed -E 's#</?changelist>##g')
  # <sha1 /> and <sha1></sha1> mean the same thing; normalise both to the empty string.
  sha=$(grep -oE '<sha1 */>|<sha1>[^<]*</sha1>' "$1" | head -1 | sed -E 's#<sha1 */>##; s#</?sha1>##g')
  echo "revision=$rev sha1=$sha changelist=$chg"
}
ROOT=$(triplet pom.xml)
DIST=$(triplet gravitee-apim-distribution/pom.xml)
echo "root:         $ROOT"
echo "distribution: $DIST"
if [ "$ROOT" != "$DIST" ]; then
  echo
  echo "The root pom and the distribution pom disagree on the version being built."
  echo "Both must be bumped together — see release/code-freeze/_common.sh and"
  echo "job-release-commit-and-prepare-next-version."
  exit 1
fi`,
      }),
      new reusable.ReusedCommand(notifyOnFailureCmd),
      new reusable.ReusedCommand(saveMavenJobCacheCmd, { jobName: ValidateJob.jobName }),
    ];
    return new Job(ValidateJob.jobName, OpenJdkExecutor.create('medium'), steps);
  }
}
