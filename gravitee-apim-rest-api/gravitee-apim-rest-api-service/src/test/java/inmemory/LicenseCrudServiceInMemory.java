/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package inmemory;

import io.gravitee.apim.core.license.crud_service.LicenseCrudService;
import io.gravitee.apim.core.license.model.License;
import java.util.*;

public class LicenseCrudServiceInMemory implements LicenseCrudService, InMemoryAlternative<License> {

    private final List<License> storage = new ArrayList<>();

    @Override
    public Optional<License> getOrganizationLicense(String organizationId) {
        return storage
            .stream()
            .filter(
                license ->
                    License.ReferenceType.ORGANIZATION.equals(license.getReferenceType()) && organizationId.equals(license.getReferenceId())
            )
            .findFirst();
    }

    @Override
    public License createOrganizationLicense(String organizationId, String license) {
        License organizationLicense = License.builder()
            .referenceType(License.ReferenceType.ORGANIZATION)
            .referenceId(organizationId)
            .license(license)
            .build();
        this.storage.add(organizationLicense);
        return organizationLicense;
    }

    @Override
    public License updateOrganizationLicense(String organizationId, String license) {
        OptionalInt index = this.findIndex(this.storage, l -> l.getReferenceId().equals(organizationId));
        if (index.isPresent()) {
            License newLicense = storage.get(index.getAsInt()).toBuilder().license(license).build();
            storage.set(index.getAsInt(), newLicense);
            return newLicense;
        }

        throw new IllegalStateException("License not found for organizationId=" + organizationId);
    }

    @Override
    public void initWith(List<License> items) {
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<License> storage() {
        return Collections.unmodifiableList(storage);
    }
}
