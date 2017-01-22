package com.justdebugit.metrics;

/**
 * User: fangjh@gmail.com
 * Date: 2017/1/14
 * Time: 22:10
 */
public class Tuple2<T1, T2> {
    private T1 first;
    private T2 second;

    public Tuple2(T1 first, T2 second) {
        this.first = first;
        this.second = second;
    }

    public T1 getFirst() {
        return first;
    }

    public T2 getSecond() {
        return second;
    }

    public static <T1, T2> Tuple2<T1, T2> of(T1 first, T2 second) {
        return new Tuple2<>(first, second);
    }
}
