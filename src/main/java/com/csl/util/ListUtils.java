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
     * Filters a list.
     * @param list list of type T to filter.
     * @param filters list of filter predicates to filter a stream of type T.
     * @return the list of T items, after filtering
     * @param <T> type of list
     */
    @SafeVarargs
    public static <T> List<T> filter(List<T> list, Predicate<T>... filters) {
        return filter(list.stream(), filters);
    }

//    /**
//     * Map and filter a list.
//     * @param list list of type T to map to type K.
//     * @param function mapping function to convert type T to type K
//     * @param filters list of filter predicates to filter a stream of type K.
//     * @return the list of K items, after the conversion and the filtering
//     * @param <T> input type
//     * @param <K> output type
//     */
//    @SafeVarargs
//    public static <T, K> List<K> mapAndFilter(List<T> list, Function<? super T, ? extends K> function, Predicate<K>... filters) {
//        return mapAndFilter(list.stream(), function, filters);
//    }

    /**
     * Map a list.
     * @param list list of type T to map to type K.
     * @param function mapping function to convert type T to type K
     * @return the list of K items, after the conversion and the filtering
     * @param <T> input type
     * @param <K> output type
     */
    public static <T, K> List<K> map(List<T> list, Function<? super T, ? extends K> function) {
        return toList(list.stream().map(function));
    }

    /**
     * Map a list.
     * @param stream stream of type T to map to type K.
     * @param function mapping function to convert type T to type K
     * @return the list of K items, after the conversion and the filtering
     * @param <T> input type
     * @param <K> output type
     */
    public static <T, K> List<K> map(Stream<T> stream, Function<? super T, ? extends K> function) {
        return toList(stream.map(function));
    }


//
//    /**
//     * Map and filter a list.
//     * @param stream stream of type T to map to type K.
//     * @param function mapping function to convert type T to type K
//     * @param filters list of filter predicates to filter a stream of type K.
//     * @return the list of K items, after the conversion and the filtering
//     * @param <T> input type
//     * @param <K> output type
//     */
//    @SafeVarargs
//    private static <T, K> List<K> mapAndFilter(Stream<T> stream, Function<? super T, ? extends K> function, Predicate<K>... filters) {
//        return filter(stream.map(function), filters);
//    }

    /**
     * Map and filter a list.
     * @param stream stream of type T to map to type K.
     * @param filters list of filter predicates to filter a stream of type K.
     * @return the list of K items, after the conversion and the filtering
     * @param <T> type of the stream
     */
    @SafeVarargs
    private static <T> List<T> filter(Stream<T> stream, Predicate<T>... filters) {
        Predicate<T> combinedFilter = filters[0];
        for (int i = 1; i < filters.length; i++) {
            combinedFilter = combinedFilter.and(filters[i]);
        }
        return toList(stream.filter(combinedFilter));
    }

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
