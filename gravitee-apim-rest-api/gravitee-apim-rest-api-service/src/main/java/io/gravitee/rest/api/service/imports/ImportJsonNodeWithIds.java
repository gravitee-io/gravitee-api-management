package io.gravitee.rest.api.service.imports;

public class ImportJsonNodeWithIds extends ImportJsonNode {

    public static final String CROSS_ID = "crossId";
    public static final String ID = "id";
    public static final String PARENT_ID = "parentId";
    public static final String API = "api";

    public ImportJsonNodeWithIds(com.fasterxml.jackson.databind.JsonNode jsonNode) {
        super(jsonNode);
    }

    public boolean hasCrossId() {
        return hasField(CROSS_ID);
    }

    public String getCrossId() {
        return getField(CROSS_ID);
    }

    public boolean hasId() {
        return hasField(ID);
    }

    public void setId(String id) {
        setField(ID, id);
    }

    public String getId() {
        return getField(ID);
    }

    public boolean hasParentId() {
        return hasField(PARENT_ID);
    }

    public String getParentId() {
        return getField(PARENT_ID);
    }

    public void setParentId(String parentId) {
        setField(PARENT_ID, parentId);
    }

    public void setApi(String id) {
        setField(API, id);
    }
}
