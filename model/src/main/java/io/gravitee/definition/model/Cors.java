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
package io.gravitee.definition.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.util.LinkedCaseInsensitiveSet;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Cors implements Serializable {

    public static int DEFAULT_ERROR_STATUS_CODE = HttpStatusCode.BAD_REQUEST_400;

    private boolean enabled;

    @JsonCreator
    public Cors(@JsonProperty("enabled") boolean enabled) {
        this.enabled = enabled;
    }

    private Set<String> accessControlAllowOrigin;

    private Set<Pattern> accessControlAllowOriginRegex;

    private Set<String> accessControlExposeHeaders;

    private int accessControlMaxAge = -1;

    private boolean accessControlAllowCredentials;

    private Set<String> accessControlAllowMethods;

    private Set<String> accessControlAllowHeaders;

    @JsonIgnore
    private int errorStatusCode = DEFAULT_ERROR_STATUS_CODE;

    private boolean runPolicies;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public static int getDefaultErrorStatusCode() {
        return DEFAULT_ERROR_STATUS_CODE;
    }

    public static void setDefaultErrorStatusCode(int defaultErrorStatusCode) {
        DEFAULT_ERROR_STATUS_CODE = defaultErrorStatusCode;
    }

    @JsonProperty("allowOrigin")
    public Set<String> getAccessControlAllowOrigin() {
        if (this.enabled && this.accessControlAllowOrigin == null) {
            return new LinkedCaseInsensitiveSet();
        }
        return accessControlAllowOrigin;
    }

    public void setAccessControlAllowOrigin(Set<String> accessControlAllowOrigin) {
        if (this.enabled) {
            this.accessControlAllowOrigin = accessControlAllowOrigin;
        }
    }

    @JsonProperty("exposeHeaders")
    public Set<String> getAccessControlExposeHeaders() {
        if (this.enabled && this.accessControlExposeHeaders == null) {
            return new LinkedCaseInsensitiveSet();
        }
        return accessControlExposeHeaders;
    }

    public void setAccessControlExposeHeaders(Set<String> accessControlExposeHeaders) {
        if (this.enabled) {
            this.accessControlExposeHeaders = accessControlExposeHeaders;
        }
    }

    @JsonProperty("maxAge")
    public int getAccessControlMaxAge() {
        return accessControlMaxAge;
    }

    public void setAccessControlMaxAge(int accessControlMaxAge) {
        if (this.enabled) {
            this.accessControlMaxAge = accessControlMaxAge;
        }
    }

    @JsonProperty("allowCredentials")
    public boolean isAccessControlAllowCredentials() {
        return accessControlAllowCredentials;
    }

    public void setAccessControlAllowCredentials(boolean accessControlAllowCredentials) {
        if (this.enabled) {
            this.accessControlAllowCredentials = accessControlAllowCredentials;
        }
    }

    @JsonProperty("allowMethods")
    public Set<String> getAccessControlAllowMethods() {
        if (this.enabled && this.accessControlAllowMethods == null) {
            return new HashSet<>();
        }
        return accessControlAllowMethods;
    }

    public void setAccessControlAllowMethods(Set<String> accessControlAllowMethods) {
        if (this.enabled) {
            this.accessControlAllowMethods = accessControlAllowMethods;
        }
    }

    @JsonProperty("allowHeaders")
    public Set<String> getAccessControlAllowHeaders() {
        if (this.enabled && this.accessControlAllowHeaders == null) {
            return new LinkedCaseInsensitiveSet();
        }
        return accessControlAllowHeaders;
    }

    public void setAccessControlAllowHeaders(Set<String> accessControlAllowHeaders) {
        if (this.enabled) {
            this.accessControlAllowHeaders = accessControlAllowHeaders;
        }
    }

    public int getErrorStatusCode() {
        return errorStatusCode;
    }

    public void setErrorStatusCode(int errorStatusCode) {
        if (this.enabled) {
            this.errorStatusCode = errorStatusCode;
        }
    }

    public Set<Pattern> getAccessControlAllowOriginRegex() {
        if (this.enabled && accessControlAllowOriginRegex == null) {
            return new HashSet<>();
        }
        return accessControlAllowOriginRegex;
    }

    @JsonIgnore
    public void setAccessControlAllowOriginRegex(Set<Pattern> accessControlAllowOriginRegex) {
        if (this.enabled) {
            this.accessControlAllowOriginRegex = accessControlAllowOriginRegex;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public boolean isRunPolicies() {
        return runPolicies;
    }

    public void setRunPolicies(boolean runPolicies) {
        if (this.enabled) {
            this.runPolicies = runPolicies;
        }
    }
}
