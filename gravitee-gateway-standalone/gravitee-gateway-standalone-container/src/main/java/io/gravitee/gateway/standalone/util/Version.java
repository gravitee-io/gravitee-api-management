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
package io.gravitee.gateway.standalone.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Represents the version information.
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public final class Version {
    /**
     * Represents the build id, which is a string like "b13" or "hudson-250".
     */
    public final String BUILD_ID;
    /**
     * Represents the complete version string, such as "JAX-WS RI 2.0-b19"
     */
    public final String BUILD_VERSION;
    /**
     * Represents the major JAX-WS version, such as "2.0".
     */
    public final String MAJOR_VERSION;

    /**
     * Represents the latest Revision number.
     */
    public final String REVISION;

    /**
     * The Runtime Version.
     */
    public static final Version RUNTIME_VERSION = Version.create(Version.class.getResourceAsStream("/version.properties"));

    private Version(String buildId, String buildVersion, String majorVersion, String revision) {
        this.BUILD_ID = fixNull(buildId);
        this.BUILD_VERSION = fixNull(buildVersion);
        this.MAJOR_VERSION = fixNull(majorVersion);
        this.REVISION = fixNull(revision);
    }

    public static Version create(InputStream is) {
        Properties props = new Properties();
        try {
            props.load(is);
        } catch (IOException e) {
            // ignore even if the property was not found. we'll treat everything as unknown
        } catch (Exception e) {
            //ignore even if property not found
        }

        return new Version(
                props.getProperty("build-id"),
                props.getProperty("build-version"),
                props.getProperty("major-version"),
                props.getProperty("revision"));
    }

    private String fixNull(String v) {
        if(v==null) return "unknown";
        return v;
    }

    public String toString() {
        return BUILD_VERSION + " revision#" + REVISION;
    }
}