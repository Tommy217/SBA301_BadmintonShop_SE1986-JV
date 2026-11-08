package fu.se.badmintonshop.modules.common.mapper;

import jakarta.persistence.*;
import org.hibernate.Hibernate;
import java.util.*;
import java.lang.reflect.*;
import java.time.LocalDateTime;

/** Scalar database-shaped DTOs for the existing frontend snapshot contract. */
public final class SnapshotMapper {
    private SnapshotMapper() {}

    public static Map<String, Object> toRow(Object entity) {
        return of(entity);
    }

    public static List<Map<String, Object>> snapshots(java.util.Collection<?> entities) {
        return entities.stream().map(SnapshotMapper::of).toList();
    }

    public static Map<String, Object> of(Object entity) {
        entity = Hibernate.unproxy(entity);
        Map<String, Object> row = new LinkedHashMap<>();
        try {
            for (Field field : entity.getClass().getDeclaredFields()) {
                Column column = field.getAnnotation(Column.class);
                JoinColumn join = field.getAnnotation(JoinColumn.class);
                if (column == null && join == null && !field.isAnnotationPresent(Id.class))
                    continue;
                if (join != null && (!join.insertable() || !join.updatable())) continue;
                String key =
                        join != null
                                ? join.name()
                                : column != null && !column.name().isEmpty()
                                        ? column.name()
                                        : field.getName();
                field.setAccessible(true);
                Object value = field.get(entity);
                if (join != null && value != null) {
                    value = Hibernate.unproxy(value);
                    Field id =
                            Arrays.stream(value.getClass().getDeclaredFields())
                                    .filter(f -> f.isAnnotationPresent(Id.class))
                                    .findFirst()
                                    .orElseThrow();
                    id.setAccessible(true);
                    value = id.get(value);
                }
                if (value instanceof LocalDateTime date) value = date.toString();
                if (value instanceof Number
                        && (key.equals("id") || key.endsWith("_id"))
                        && !Set.of("variant_id", "product_id", "voucher_id").contains(key))
                    value = value.toString();
                row.put(key, value);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot map snapshot", e);
        }
        return row;
    }
}
