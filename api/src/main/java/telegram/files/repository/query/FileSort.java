package telegram.files.repository.query;

import cn.hutool.core.util.StrUtil;

import java.util.Set;

public record FileSort(String column, String direction) {

    private static final String DEFAULT_COLUMN = "message_id";
    private static final Set<String> SUPPORTED_COLUMNS = Set.of(
            DEFAULT_COLUMN,
            "date",
            "completion_date",
            "size",
            "reaction_count");

    public static FileSort from(String sort, String order) {
        String column = StrUtil.blankToDefault(sort, DEFAULT_COLUMN);
        if (!SUPPORTED_COLUMNS.contains(column)) {
            throw new IllegalArgumentException("Unsupported file sort field: " + column);
        }

        String direction = "asc".equals(order) ? "ASC" : "DESC";
        return new FileSort(column, direction);
    }

    public boolean isCustom() {
        return !DEFAULT_COLUMN.equals(column);
    }
}
