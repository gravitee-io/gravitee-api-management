# policy-assembly.xml (Maven Assembly Descriptor)

Located at `src/assembly/policy-assembly.xml`. This file is **identical across all Kafka native policies** - copy it verbatim.

## Template

```xml
<!--

    Copyright (C) 2015 The Gravitee team (http://gravitee.io)

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
<assembly xmlns="http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.3"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.3 http://maven.apache.org/xsd/assembly-1.1.3.xsd">
    <id>policy</id>
    <formats>
        <format>zip</format>
    </formats>
    <includeBaseDirectory>false</includeBaseDirectory>

    <!-- Include the main Policy Jar file -->
    <files>
        <file>
            <source>${project.build.directory}/${project.build.finalName}.jar</source>
        </file>
    </files>

    <fileSets>
        <!-- Then include Policy configuration schemas -->
        <fileSet>
            <directory>src/main/resources/schemas</directory>
            <outputDirectory>schemas</outputDirectory>
        </fileSet>

        <fileSet>
            <directory>${basedir}</directory>
            <includes>
                <include>DOCUMENTATION.adoc</include>
            </includes>
            <outputDirectory>docs</outputDirectory>
        </fileSet>

        <fileSet>
            <directory>${basedir}</directory>
            <includes>
                <include>${icon}</include>
            </includes>
        </fileSet>

        <!-- Create the empty lib directory in case of no libraries is required -->
        <fileSet>
            <directory>${project.basedir}/src/assembly</directory>
            <outputDirectory>lib</outputDirectory>
            <excludes>
                <exclude>*</exclude>
            </excludes>
        </fileSet>
    </fileSets>

    <!-- Finally include Policy dependencies -->
    <dependencySets>
        <dependencySet>
            <outputDirectory>lib</outputDirectory>
            <useProjectArtifact>false</useProjectArtifact>
        </dependencySet>
    </dependencySets>
</assembly>
```

## Output Structure

Running `mvn package` produces a ZIP file with this structure:

```
gravitee-policy-kafka-<name>-<version>.zip
├── gravitee-policy-kafka-<name>-<version>.jar    (compiled policy)
├── schemas/
│   └── schema-form.json                          (configuration schema)
├── docs/
│   └── DOCUMENTATION.adoc                        (documentation)
└── lib/                                          (runtime dependencies, if any)
```

Deploy this ZIP to the gateway's `plugins/` directory.
