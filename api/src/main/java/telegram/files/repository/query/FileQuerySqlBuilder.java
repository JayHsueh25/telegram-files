package telegram.files.repository.query;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import telegram.files.MessyUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public final class FileQuerySqlBuilder {

    private FileQuerySqlBuilder() {
    }

    public static Query build(long chatId, FileQueryFilter filter) {
        FileQueryFilter queryFilter = filter == null ? FileQueryFilter.from(Map.of()) : filter;
        Map<String, Object> params = new HashMap<>();
        params.put("limit", queryFilter.limit());

        StringBuilder whereClause = new StringBuilder("type != 'thumbnail'");
        appendChatFilter(chatId, whereClause, params);
        appendSearchFilter(queryFilter, whereClause, params);
        appendTypeFilter(queryFilter, whereClause, params);
        appendStatusFilters(queryFilter, whereClause, params);
        appendTagFilters(queryFilter, whereClause, params);
        appendMessageThreadFilter(queryFilter, whereClause, params);
        appendDateRangeFilter(queryFilter, whereClause, params);
        appendSizeRangeFilter(queryFilter, whereClause, params);
        appendSortFilter(queryFilter, whereClause);

        String countClause = whereClause.toString();
        Map<String, Object> countParams = new HashMap<>(params);
        countParams.remove("limit");
        appendPaginationFilter(queryFilter, whereClause, params);

        return new Query(whereClause.toString(), countClause, orderBy(queryFilter), Map.copyOf(params), Map.copyOf(countParams));
    }

    private static void appendChatFilter(long chatId, StringBuilder whereClause, Map<String, Object> params) {
        if (chatId != 0) {
            whereClause.append(" AND chat_id = #{chatId}");
            params.put("chatId", chatId);
        }
    }

    private static void appendSearchFilter(FileQueryFilter filter, StringBuilder whereClause, Map<String, Object> params) {
        if (StrUtil.isNotBlank(filter.search())) {
            whereClause.append(" AND (file_name LIKE #{search} OR caption LIKE #{search})");
            params.put("search", "%%" + filter.search() + "%%");
        }
    }

    private static void appendTypeFilter(FileQueryFilter filter, StringBuilder whereClause, Map<String, Object> params) {
        if (StrUtil.isNotBlank(filter.type()) && !Objects.equals(filter.type(), "all")) {
            if (Objects.equals(filter.type(), "media")) {
                whereClause.append(" AND type IN ('photo', 'video')");
            } else {
                whereClause.append(" AND type = #{type}");
                params.put("type", filter.type());
            }
        }
    }

    private static void appendStatusFilters(FileQueryFilter filter, StringBuilder whereClause, Map<String, Object> params) {
        if (StrUtil.isNotBlank(filter.downloadStatus())) {
            whereClause.append(" AND download_status = #{downloadStatus}");
            params.put("downloadStatus", filter.downloadStatus());
        }
        if (StrUtil.isNotBlank(filter.transferStatus())) {
            whereClause.append(" AND transfer_status = #{transferStatus}");
            params.put("transferStatus", filter.transferStatus());
        }
    }

    private static void appendTagFilters(FileQueryFilter filter, StringBuilder whereClause, Map<String, Object> params) {
        if (filter.tags().isEmpty()) {
            return;
        }

        StringJoiner tagClause = new StringJoiner(" OR ");
        for (int i = 0; i < filter.tags().size(); i++) {
            String paramName = "tag" + i;
            tagClause.add("tags LIKE #{" + paramName + "}");
            params.put(paramName, "%" + filter.tags().get(i) + "%");
        }
        whereClause.append(" AND (").append(tagClause).append(")");
    }

    private static void appendMessageThreadFilter(FileQueryFilter filter, StringBuilder whereClause, Map<String, Object> params) {
        if (filter.messageThreadId() != 0) {
            whereClause.append(" AND message_thread_id = #{messageThreadId}");
            params.put("messageThreadId", filter.messageThreadId());
        }
    }

    private static void appendDateRangeFilter(FileQueryFilter filter, StringBuilder whereClause, Map<String, Object> params) {
        if (StrUtil.isBlank(filter.dateType()) || StrUtil.isBlank(filter.dateRange())) {
            return;
        }

        String[] dates = filter.dateRange().split(",");
        if (dates.length != 2) {
            return;
        }

        long startTime = LocalDate.parse(dates[0], DateTimeFormatter.ISO_DATE)
                .atStartOfDay()
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endTime = LocalDate.parse(dates[1], DateTimeFormatter.ISO_DATE)
                .atTime(LocalTime.MAX)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        if (Objects.equals(filter.dateType(), "sent")) {
            whereClause.append(" AND date >= #{startTime} AND date <= #{endTime}");
            startTime = startTime / 1000;
            endTime = endTime / 1000;
        } else {
            whereClause.append(" AND completion_date >= #{startTime} AND completion_date <= #{endTime}");
        }
        params.put("startTime", startTime);
        params.put("endTime", endTime);
    }

    private static void appendSizeRangeFilter(FileQueryFilter filter, StringBuilder whereClause, Map<String, Object> params) {
        if (StrUtil.isBlank(filter.sizeRange()) || StrUtil.isBlank(filter.sizeUnit())) {
            return;
        }

        String[] sizes = filter.sizeRange().split(",");
        if (sizes.length != 2) {
            return;
        }

        long minSize = MessyUtils.convertToByte(Convert.toLong(sizes[0]), filter.sizeUnit());
        long maxSize = MessyUtils.convertToByte(Convert.toLong(sizes[1]), filter.sizeUnit());
        whereClause.append(" AND size >= #{minSize} AND size <= #{maxSize}");
        params.put("minSize", minSize);
        params.put("maxSize", maxSize);
    }

    private static void appendSortFilter(FileQueryFilter filter, StringBuilder whereClause) {
        if (Objects.equals(filter.sort().column(), "completion_date")) {
            whereClause.append(" AND completion_date IS NOT NULL");
        }
    }

    private static void appendPaginationFilter(FileQueryFilter filter, StringBuilder whereClause, Map<String, Object> params) {
        if (filter.fromMessageId() <= 0) {
            return;
        }

        params.put("fromMessageId", filter.fromMessageId());
        if (Objects.equals(filter.sort().column(), "message_id")) {
            String comparator = filter.sort().isAscending() ? ">" : "<";
            whereClause.append(" AND message_id ").append(comparator).append(" #{fromMessageId}");
        } else if (filter.sort().isCustom()) {
            params.put("fromSortField", filter.fromSortField());
            String comparator = filter.sort().isAscending() ? ">" : "<";
            String column = filter.sort().column();
            whereClause.append(" AND (")
                    .append(column).append(" ").append(comparator).append(" #{fromSortField}")
                    .append(" OR (").append(column).append(" = #{fromSortField} AND message_id < #{fromMessageId}))");
        } else {
            whereClause.append(" AND message_id < #{fromMessageId}");
        }
    }

    private static String orderBy(FileQueryFilter filter) {
        String orderBy = "%s %s".formatted(filter.sort().column(), filter.sort().direction());
        if (Objects.equals(filter.sort().column(), "message_id")) {
            return orderBy;
        }
        return orderBy + ", message_id DESC";
    }

    public record Query(String whereClause, String countClause, String orderBy, Map<String, Object> params,
                        Map<String, Object> countParams) {
    }
}
