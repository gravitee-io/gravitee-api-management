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
package io.gravitee.definition.model.services.dynamicproperty.http;

import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyProviderConfiguration;
import java.util.Objects;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpDynamicPropertyProviderConfiguration
  implements DynamicPropertyProviderConfiguration {

  private String url;

  private String specification;

  private boolean useSystemProxy;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getSpecification() {
    return specification;
  }

  public void setSpecification(String specification) {
    this.specification = specification;
  }

  public boolean isUseSystemProxy() {
    return useSystemProxy;
  }

  public void setUseSystemProxy(boolean useSystemProxy) {
    this.useSystemProxy = useSystemProxy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HttpDynamicPropertyProviderConfiguration that = (HttpDynamicPropertyProviderConfiguration) o;
    return (
      useSystemProxy == that.useSystemProxy &&
      Objects.equals(url, that.url) &&
      Objects.equals(specification, that.specification)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(url, specification, useSystemProxy);
  }
}
