package io.gravitee.rest.api.service.converter;

import static org.junit.Assert.*;

import io.gravitee.rest.api.model.*;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PageConverterTest {

    @InjectMocks
    private PageConverter pageConverter;

    @Test
    public void toUpdatePageEntity_should_convert_to_UpdatePageEntity() {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setId("page#2");
        pageEntity.setName("Sub Page 3");
        pageEntity.setType("ASCIIDOC");
        pageEntity.setParentId("page#1");
        pageEntity.setReferenceType("API");
        pageEntity.setReferenceId("api#1");
        pageEntity.setConfiguration(Collections.emptyMap());
        pageEntity.setContent("content");
        pageEntity.setExcludedAccessControls(true);
        pageEntity.setAccessControls(Collections.emptySet());
        pageEntity.setHomepage(false);
        pageEntity.setLastContributor("contributor");
        pageEntity.setOrder(1);
        pageEntity.setPublished(true);
        PageSourceEntity pageSourceEntity = new PageSourceEntity();
        pageSourceEntity.setType("API");
        pageEntity.setSource(pageSourceEntity);
        pageEntity.setAttachedMedia(Collections.emptyList());

        UpdatePageEntity updatePageEntity = pageConverter.toUpdatePageEntity(pageEntity);

        assertEquals("Sub Page 3", updatePageEntity.getName());
        assertEquals("page#1", updatePageEntity.getParentId());
        assertEquals(Collections.emptyMap(), updatePageEntity.getConfiguration());
        assertEquals("content", updatePageEntity.getContent());
        assertTrue(updatePageEntity.isExcludedAccessControls());
        assertEquals(Collections.emptySet(), updatePageEntity.getAccessControls());
        assertFalse(updatePageEntity.isHomepage());
        assertEquals("contributor", updatePageEntity.getLastContributor());
        assertEquals((Integer) 1, updatePageEntity.getOrder());
        assertTrue(updatePageEntity.isPublished());
        assertEquals("API", updatePageEntity.getSource().getType());
        assertEquals(Collections.emptyList(), updatePageEntity.getAttachedMedia());
    }

    @Test
    public void toNewPageEntity_should_convert_to_NewPageEntity() {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setId("page#2");
        pageEntity.setName("Sub Page 3");
        pageEntity.setType("ASCIIDOC");
        pageEntity.setParentId("page#1");
        pageEntity.setReferenceType("API");
        pageEntity.setReferenceId("api#1");
        pageEntity.setConfiguration(Collections.emptyMap());
        pageEntity.setContent("content");
        pageEntity.setExcludedAccessControls(true);
        pageEntity.setAccessControls(Collections.emptySet());
        pageEntity.setHomepage(false);
        pageEntity.setLastContributor("contributor");
        pageEntity.setOrder(1);
        pageEntity.setPublished(true);
        PageSourceEntity pageSourceEntity = new PageSourceEntity();
        pageSourceEntity.setType("API");
        pageEntity.setSource(pageSourceEntity);
        pageEntity.setAttachedMedia(Collections.emptyList());

        NewPageEntity newPageEntity = pageConverter.toNewPageEntity(pageEntity);

        assertEquals("Sub Page 3", newPageEntity.getName());
        assertEquals("page#1", newPageEntity.getParentId());
        assertEquals(PageType.ASCIIDOC, newPageEntity.getType());
        assertEquals(Collections.emptyMap(), newPageEntity.getConfiguration());
        assertEquals("content", newPageEntity.getContent());
        assertTrue(newPageEntity.isExcludedAccessControls());
        assertEquals(Collections.emptySet(), newPageEntity.getAccessControls());
        assertFalse(newPageEntity.isHomepage());
        assertEquals("contributor", newPageEntity.getLastContributor());
        assertEquals((Integer) 1, (Integer) newPageEntity.getOrder());
        assertTrue(newPageEntity.isPublished());
        assertEquals("API", newPageEntity.getSource().getType());
        assertEquals(Collections.emptyList(), newPageEntity.getAttachedMedia());
    }
}
