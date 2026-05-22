package telegram.files.repository.query;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileQuerySqlBuilderTest {

    @Test
    void parameterizesTagFilters() {
        FileQuerySqlBuilder.Query query = FileQuerySqlBuilder.build(0, filter(Map.of("tags", "movie,photo")));

        assertTrue(query.whereClause().contains("tags LIKE #{tag0}"));
        assertTrue(query.whereClause().contains("tags LIKE #{tag1}"));
        assertEquals("%movie%", query.params().get("tag0"));
        assertEquals("%photo%", query.params().get("tag1"));
    }

    @Test
    void usesWhitelistedSortColumnAndDirection() {
        FileQuerySqlBuilder.Query query = FileQuerySqlBuilder.build(0, filter(Map.of(
                "sort", "size",
                "order", "asc")));

        assertEquals("size ASC", query.orderBy());
    }

    @Test
    void completionDateSortExcludesNullCompletionDateFromRowsAndCount() {
        FileQuerySqlBuilder.Query query = FileQuerySqlBuilder.build(0, filter(Map.of(
                "sort", "completion_date",
                "order", "desc")));

        assertTrue(query.whereClause().contains("completion_date IS NOT NULL"));
        assertTrue(query.countClause().contains("completion_date IS NOT NULL"));
    }

    @Test
    void defaultPaginationDoesNotAffectCountClause() {
        FileQuerySqlBuilder.Query query = FileQuerySqlBuilder.build(0, filter(Map.of("fromMessageId", "123")));

        assertTrue(query.whereClause().contains("message_id < #{fromMessageId}"));
        assertFalse(query.countClause().contains("fromMessageId"));
        assertFalse(query.countClause().contains("message_id < #{fromMessageId}"));
    }

    @Test
    void ascendingCustomSortPaginationUsesParameterizedSortCursor() {
        FileQuerySqlBuilder.Query query = FileQuerySqlBuilder.build(0, filter(Map.of(
                "sort", "size",
                "order", "asc",
                "fromMessageId", "123",
                "fromSortField", "456")));

        assertTrue(query.whereClause().contains("size > #{fromSortField}"));
        assertTrue(query.whereClause().contains("size = #{fromSortField}"));
        assertFalse(query.whereClause().contains("size > 456"));
        assertFalse(query.whereClause().contains("size = 456"));
        assertEquals(456L, query.params().get("fromSortField"));
    }

    @Test
    void descendingCustomSortPaginationUsesParameterizedSortCursor() {
        FileQuerySqlBuilder.Query query = FileQuerySqlBuilder.build(0, filter(Map.of(
                "sort", "date",
                "order", "desc",
                "fromMessageId", "123",
                "fromSortField", "456")));

        assertTrue(query.whereClause().contains("date < #{fromSortField}"));
        assertTrue(query.whereClause().contains("date = #{fromSortField}"));
        assertFalse(query.whereClause().contains("date < 456"));
        assertFalse(query.whereClause().contains("date = 456"));
        assertEquals(456L, query.params().get("fromSortField"));
    }

    private static FileQueryFilter filter(Map<String, String> values) {
        return FileQueryFilter.from(values);
    }
}
