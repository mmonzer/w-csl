package com.csl.util;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utils to work with lists.
 */
public class ListUtils {
    private ListUtils() {}

    /**
     * Converts stream to List.
     * @param stream stream to convert
     * @return list from stream
     * @param <T> type of list and stream
     */
    public static <T> List<T> toList(Stream<T> stream)  {
        // Although recommended to use .toList() instead of .collect(Collectors.toList()) the first one creates
        // an immutable list, whereas the second one is not immutable, allowing adding items afterwords (needed).
        return stream.collect(Collectors.toList()); // TODO: replace by stream.collect(Collectors.toCollection(ArrayList::new));
    }
}
