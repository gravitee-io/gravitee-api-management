/**
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
package io.gravitee.repository.jdbc;

public enum DatabaseConfigurationEnum {
    MARIADB("mariadb", "10.3.6"),
    MYSQL("mysql", "5.7.22"),
    SQLSERVER("mcr.microsoft.com/mssql/server", "2017-CU12"),
    POSTGRESQL("postgres", "9.6.12");

    private String dockerImageName;
    private String defaultTag;

    public String getDockerImageName() {
        return dockerImageName;
    }

    public String getDefaultTag() {
        return defaultTag;
    }

    DatabaseConfigurationEnum(String dockerImageName, String defaultTag) {
        this.dockerImageName = dockerImageName;
        this.defaultTag = defaultTag;
    }
}
