package fr.catcore.modremapperapi.utils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Deprecated
public class CollectionUtils {

    public static <T> Collection<T> transformList(Collection<T> collection, Function<T, T> function) {
        return collection.stream().map(function).collect(Collectors.toList());
    }

    public static <K, T> Map<K, T> transformMapValues(Map<K, T> ktMap, Function<T, T> function) {
        return ktMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> function.apply(e.getValue())));
    }
}
