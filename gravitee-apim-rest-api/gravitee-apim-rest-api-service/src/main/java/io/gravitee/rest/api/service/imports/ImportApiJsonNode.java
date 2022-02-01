package io.gravitee.rest.api.service.imports;

import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.List;

public class ImportApiJsonNode extends ImportJsonNodeWithIds {

    public static final String PLANS = "plans";
    public static final String PAGES = "pages";
    public static final String METADATA = "metadata";
    public static final String VIEWS = "views";
    public static final String API_MEDIA = "apiMedia";
    public static final String MEMBERS = "members";

    public ImportApiJsonNode(JsonNode jsonNode) {
        super(jsonNode);
    }

    public boolean hasPlans() {
        return hasArray(PLANS);
    }

    public List<ImportJsonNodeWithIds> getPlans() {
        return getChildNodesWithIdByName(PLANS);
    }

    public ArrayNode getPlansArray() {
        return getArray(PLANS);
    }

    public boolean hasPages() {
        return hasArray(PAGES);
    }

    public List<ImportJsonNodeWithIds> getPages() {
        return getChildNodesWithIdByName(PAGES);
    }

    public ArrayNode getPagesArray() {
        return getArray(PAGES);
    }

    public List<ImportJsonNode> getMetadata() {
        return getChildExtendedNodesByName(METADATA);
    }

    public List<ImportJsonNode> getViews() {
        return getChildExtendedNodesByName(VIEWS);
    }

    public List<ImportJsonNode> getMedia() {
        return getChildExtendedNodesByName(API_MEDIA);
    }

    public List<ImportJsonNode> getMembers() {
        return getChildExtendedNodesByName(MEMBERS);
    }

    public boolean hasMembers() {
        return hasArray(MEMBERS);
    }

    private List<ImportJsonNodeWithIds> getChildNodesWithIdByName(String fieldName) {
        return getChildNodesByName(fieldName).stream().map(ImportJsonNodeWithIds::new).collect(toList());
    }

    private List<ImportJsonNode> getChildExtendedNodesByName(String fieldName) {
        return getChildNodesByName(fieldName).stream().map(ImportJsonNode::new).collect(toList());
    }
}
