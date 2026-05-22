package telegram.files.repository.query;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.Map;

public record FileQueryFilter(
        String search,
        String type,
        String downloadStatus,
        String transferStatus,
        List<String> tags,
        long messageThreadId,
        String dateType,
        String dateRange,
        String sizeRange,
        String sizeUnit,
        long fromMessageId,
        long fromSortField,
        int limit,
        FileSort sort) {

    public FileQueryFilter {
        tags = tags == null ? List.of() : List.copyOf(tags);
        sort = sort == null ? FileSort.from(null, null) : sort;
    }

    public static FileQueryFilter from(Map<String, String> filter) {
        Map<String, String> values = filter == null ? Map.of() : filter;
        return new FileQueryFilter(
                values.get("search"),
                values.get("type"),
                values.get("downloadStatus"),
                values.get("transferStatus"),
                parseTags(values.get("tags")),
                Convert.toLong(values.get("messageThreadId"), 0L),
                values.get("dateType"),
                values.get("dateRange"),
                values.get("sizeRange"),
                values.get("sizeUnit"),
                Convert.toLong(values.get("fromMessageId"), 0L),
                Convert.toLong(values.get("fromSortField"), 0L),
                Convert.toInt(values.get("limit"), 20),
                FileSort.from(values.get("sort"), values.get("order")));
    }

    private static List<String> parseTags(String tags) {
        return StrUtil.split(tags, ",").stream()
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .toList();
    }
}
