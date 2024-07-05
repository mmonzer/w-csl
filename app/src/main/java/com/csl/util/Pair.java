package com.csl.util;

import java.util.function.BiFunction;

/**
 * Immutable object representing a pair or objects.
 *
 * @param <T> The type of the first element of the pair.
 * @param <U> The type of the second element of the pair.
 */
public class Pair<T,U> {
    private final T first;
    private final U second;

    /**
     * Create a pair of objects.
     *
     * @param first The first element of the pair.
     * @param second The second element of the pair.
     */
    public Pair(T first, U second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Get the first element of the pair.
     * @return The first element of the pair.
     */
    public T getFirst() {
        return first;
    }

    /**
     * Get the second element of the pair.
     * @return The second element of the pair.
     */
    public U getSecond() {
        return second;
    }

    /**
     * Create a new pair with the specified first, and the same second.
     *
     * @param newFirst The new object to put in first position.
     * @return A new pair (newFirst, second).
     */
    public Pair<T,U> setFirst(T newFirst) {
        return new Pair<>(newFirst, second);
    }

    /**
     * Create a new pair with the same first and the specified second.
     *
     * @param newSecond The new object to put in second position.
     * @return A new pair (first, newSecond).
     */
    public Pair<T,U> setSecond(U newSecond) {
        return new Pair<>(first, newSecond);
    }

    /**
     * Apply a function to the pair.
     *
     * @param f The function to apply.
     * @param <V> The type of the first element of the resulting pair.
     * @param <W> The type of the second element of the resulting pair.
     * @return The result of applying the function to the pair.
     */
    public <V,W> Pair<V,W> map(BiFunction<T,U,Pair<V,W>> f) {
        return f.apply(first, second);
    }
}
