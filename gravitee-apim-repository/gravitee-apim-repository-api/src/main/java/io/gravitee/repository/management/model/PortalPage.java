package io.gravitee.repository.management.model;

import java.util.List;
import java.util.Objects;

public class PortalPage {
    private String id;
    private String content;
    private List<String> contexts;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getContexts() {
        return contexts;
    }

    public void setContexts(List<String> contexts) {
        this.contexts = contexts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortalPage that = (PortalPage) o;
        return Objects.equals(id, that.id) && Objects.equals(content, that.content) && Objects.equals(contexts, that.contexts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, content, contexts);
    }
}

