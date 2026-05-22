package telegram.files.repository.query;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileQueryFilterTest {

    @Test
    void rejectsUnsupportedSortField() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> FileSort.from("created_at", "asc"));

        assertEquals("Unsupported file sort field: created_at", exception.getMessage());
    }

    @Test
    void emptyFilterUsesMessageIdDescendingSort() {
        FileQueryFilter filter = FileQueryFilter.from(Map.of());

        assertEquals(20, filter.limit());
        assertEquals(0L, filter.fromMessageId());
        assertEquals(0L, filter.fromSortField());
        assertEquals("message_id", filter.sort().column());
        assertEquals("DESC", filter.sort().direction());
        assertFalse(filter.sort().isCustom());
    }

    @Test
    void tagsAreTrimmedAndBlankValuesAreRemoved() {
        FileQueryFilter filter = FileQueryFilter.from(Map.of("tags", "movie,, photo "));

        assertEquals(2, filter.tags().size());
        assertEquals("movie", filter.tags().get(0));
        assertEquals("photo", filter.tags().get(1));
    }

    @Test
    void onlyAscOrderOutputsAscendingDirection() {
        assertEquals("ASC", FileSort.from("date", "asc").direction());
        assertEquals("DESC", FileSort.from("date", "ASC").direction());
        assertEquals("DESC", FileSort.from("date", "desc").direction());
        assertEquals("DESC", FileSort.from("date", "").direction());
        assertEquals("DESC", FileSort.from("date", null).direction());
        assertTrue(FileSort.from("date", "asc").isCustom());
    }

    @Test
    void messageIdAscendingIsCustomSort() {
        FileSort sort = FileSort.from("message_id", "asc");

        assertEquals("ASC", sort.direction());
        assertTrue(sort.isCustom());
        assertFalse(sort.isDefaultSort());
    }

    @Test
    void messageIdDescendingIsDefaultSort() {
        FileSort sort = FileSort.from("message_id", null);

        assertEquals("DESC", sort.direction());
        assertTrue(sort.isDefaultSort());
        assertFalse(sort.isCustom());
    }

    @Test
    void constructorDefensivelyCopiesTags() {
        List<String> tags = new ArrayList<>(List.of("movie"));
        FileQueryFilter filter = newFilter(tags, FileSort.from("date", "asc"));

        tags.add("photo");

        assertEquals(List.of("movie"), filter.tags());
    }

    @Test
    void constructorNormalizesNullTagsAndSort() {
        FileQueryFilter filter = newFilter(null, null);

        assertEquals(List.of(), filter.tags());
        assertEquals("message_id", filter.sort().column());
        assertEquals("DESC", filter.sort().direction());
        assertTrue(filter.sort().isDefaultSort());
    }

    private static FileQueryFilter newFilter(List<String> tags, FileSort sort) {
        return new FileQueryFilter(
                null,
                null,
                null,
                null,
                tags,
                0L,
                null,
                null,
                null,
                null,
                0L,
                0L,
                20,
                sort);
    }
}
