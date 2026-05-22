package telegram.files.repository.query;

import org.junit.jupiter.api.Test;

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
}
