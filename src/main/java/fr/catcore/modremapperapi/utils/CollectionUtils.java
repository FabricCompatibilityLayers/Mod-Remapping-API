package fr.catcore.modremapperapi.utils;

import java.util.*;
import java.util.function.Function;

@Deprecated
public class CollectionUtils {

    public static <T> Collection<T> transformList(Collection<T> collection, Function<T, T> function) {
        List<T> list = new ArrayList<>();
        for (T t : collection) {
            list.add(function.apply(t));
        }

        return list;
    }

    public static <K, T> Map<K, T> transformMapValues(Map<K, T> ktMap, Function<T, T> function) {
        Map<K, T> map = new HashMap<>();

        for (Map.Entry<K, T> entry : ktMap.entrySet()) {
            map.put(entry.getKey(), function.apply(entry.getValue()));
        }

        return map;
    }
}
