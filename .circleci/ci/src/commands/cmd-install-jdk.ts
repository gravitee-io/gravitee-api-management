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
import { Command, Config, Executor, commands, executors, reusable } from '../circleci-config';
import { config } from '../config';

/**
 * Installs the JDK the project builds with on a machine executor.
 *
 * Docker jobs get their JDK from the `cimg/openjdk` tag, but machine jobs run on
 * the Ubuntu VM image and use whatever it ships. As of the Q2 2026 images that is
 * OpenJDK 21 on every LTS — 22.04, 24.04 and 26.04 alike — so moving to a newer
 * Ubuntu does not help: the JDK has to be installed.
 *
 * Only needed by the machine jobs that invoke Maven. The e2e jobs run yarn and
 * Docker only, and the packaging jobs either sit on a Docker executor or shell out
 * to `docker run` without touching Java.
 */
export class InstallJdkCommand {
  private static commandName = 'cmd-install-jdk';

  public static get(): reusable.ReusableCommand {
    // Track the major the Docker executors use, so both stay on the same JDK.
    const feature = config.executor.openjdk.version.split('.')[0];
    const cacheKey = `${config.cache.prefix}-temurin-${feature}`;

    return new reusable.ReusableCommand(
      InstallJdkCommand.commandName,
      [
        new commands.cache.Restore({ keys: [cacheKey] }),
        new commands.Run({
          name: `Install JDK ${feature}`,
          command: `JDK_HOME=/usr/lib/jvm/temurin-${feature}
CACHE_DIR="\${HOME}/.cache/temurin"
TARBALL="\${CACHE_DIR}/temurin-${feature}.tar.gz"

if java -version 2>&1 | head -1 | grep -q '"${feature}\\.'; then
  # Still export JAVA_HOME, so both paths leave the environment in the same state.
  RESOLVED_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")"
  echo "JDK ${feature} already installed at \${RESOLVED_HOME}"
  echo "export JAVA_HOME=\${RESOLVED_HOME}" >> "$BASH_ENV"
  exit 0
fi

# x64 is hardcoded because MachineResourceClass models no Arm variant, so a
# machine job cannot run on Arm without widening that type first.
# Resolve the latest GA of the feature release rather than pinning a build number:
# patch drift inside a major is harmless here, a stale pin is not. The cache key
# holds a major, so a pipeline that restores the cache stays on one patch anyway.
mkdir -p "\${CACHE_DIR}"

if [ -f "\${TARBALL}" ]; then
  echo "Reusing the cached Temurin ${feature} archive"
else
  META="$(curl --retry 3 --retry-all-errors --retry-delay 5 -fsSL \\
    "https://api.adoptium.net/v3/assets/latest/${feature}/hotspot?os=linux&architecture=x64&image_type=jdk")"
  JDK_URL="$(echo "\${META}" | jq -r '.[0].binary.package.link')"
  JDK_SHA="$(echo "\${META}" | jq -r '.[0].binary.package.checksum')"

  echo "Downloading $(echo "\${META}" | jq -r '.[0].version.semver')"
  curl --retry 3 --retry-all-errors --retry-delay 5 -fsSL "\${JDK_URL}" -o "\${TARBALL}"
  # HTTPS gives transport security, not artifact provenance: verify before extracting as root.
  echo "\${JDK_SHA}  \${TARBALL}" | sha256sum -c -
fi

echo "Replacing $(java -version 2>&1 | head -1)"
sudo mkdir -p "\${JDK_HOME}"
sudo tar -xzf "\${TARBALL}" -C "\${JDK_HOME}" --strip-components=1 --no-same-owner

echo "export JAVA_HOME=\${JDK_HOME}" >> "$BASH_ENV"
echo "export PATH=\${JDK_HOME}/bin:\\$PATH" >> "$BASH_ENV"
source "$BASH_ENV"

java -version`,
        }),
        new commands.cache.Save({
          key: cacheKey,
          paths: ['~/.cache/temurin'],
          when: 'on_success',
        }),
      ],
      undefined,
      `Install the JDK on machine executors, which ship an older one`,
    );
  }
}

/**
 * Steps a job needs to run Maven on the right JDK, given its executor.
 *
 * Returns nothing for Docker executors, which already carry the JDK through their
 * image. Call it from every job that invokes Maven and the rule holds by
 * construction, rather than by remembering to wire the command in.
 */
export function withJdk(dynamicConfig: Config, executor: Executor): Command[] {
  if (!(executor instanceof executors.MachineExecutor)) {
    return [];
  }

  const installJdkCmd = InstallJdkCommand.get();
  dynamicConfig.addReusableCommand(installJdkCmd);

  return [new reusable.ReusedCommand(installJdkCmd)];
}
