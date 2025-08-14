package inmemory;

import io.gravitee.apim.core.cockpit.crud_service.InstallationCrudService;
import java.util.List;
import java.util.Optional;
import lombok.Setter;

public class InstallationCrudServiceInMemory implements InstallationCrudService, InMemoryAlternative<String> {

    @Setter
    private String installationId = null;

    @Override
    public Optional<String> getInstallationId() {
        return Optional.ofNullable(installationId);
    }

    @Override
    public void initWith(List<String> items) {
        this.installationId = items.isEmpty() ? null : items.getFirst();
    }

    @Override
    public void reset() {
        this.installationId = null;
    }

    @Override
    public List<String> storage() {
        return installationId != null ? List.of(installationId) : List.of();
    }
}
