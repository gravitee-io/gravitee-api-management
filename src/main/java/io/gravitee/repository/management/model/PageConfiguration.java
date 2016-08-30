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
package io.gravitee.repository.management.model;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PageConfiguration {
    private String tryItURL;
    private boolean tryIt;

    public String getTryItURL() {
        return tryItURL;
    }

    public void setTryItURL(String tryItURL) {
        this.tryItURL = tryItURL;
    }

    public boolean isTryIt() {
        return tryIt;
    }

    public void setTryIt(boolean tryIt) {
        this.tryIt = tryIt;
    }

    @Override
    public String toString() {
        return "PageConfiguration{" +
                "tryItURL='" + tryItURL + '\'' +
                ", tryIt='" + tryIt + '\'' +
                '}';
    }
}
