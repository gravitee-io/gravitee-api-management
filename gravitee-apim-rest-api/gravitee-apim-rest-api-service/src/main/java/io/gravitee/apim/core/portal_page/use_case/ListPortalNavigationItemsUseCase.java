package io.gravitee.apim.core.portal_page.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalPageNavigationId;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListPortalNavigationItemsUseCase {

    private final PortalNavigationItemsQueryService queryService;

    public Output execute(Input input) {
        var directItems = input
            .parentId()
            .map(parentId -> queryService.findByParentIdAndEnvironmentId(input.environmentId(), parentId))
            .orElse(queryService.findTopLevelItemsByEnvironmentId(input.environmentId(), input.portalArea()));

        var items = new ArrayList<>(directItems);

        if (input.loadChildren()) {
            for (var item : directItems) {
                if (item instanceof PortalNavigationFolder) {
                    addChildrenRecursively(items, item.getId(), input.environmentId());
                }
            }
        }

        return new Output(items);
    }

    private void addChildrenRecursively(List<PortalNavigationItem> items, PortalPageNavigationId parentId, String environmentId) {
        var children = queryService.findByParentIdAndEnvironmentId(environmentId, parentId);
        for (var child : children) {
            items.add(child);
            if (child instanceof PortalNavigationFolder) {
                addChildrenRecursively(items, child.getId(), environmentId);
            }
        }
    }

    public record Output(List<PortalNavigationItem> items) {}

    public record Input(
        String environmentId,
        String organizationId,
        PortalArea portalArea,
        Optional<PortalPageNavigationId> parentId,
        boolean loadChildren
    ) {}
}
