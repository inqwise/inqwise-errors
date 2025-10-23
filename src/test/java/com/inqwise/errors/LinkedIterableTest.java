package com.inqwise.errors;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

class LinkedIterableTest {

    @Test
    void iteratorTraversesUntilTerminated() {
        var iterable = LinkedIterable.over(1, (Integer value) -> value == null, value -> {
            if (value >= 3) {
                return null;
            }
            return value + 1;
        });

        var collected = new ArrayList<Integer>();
        iterable.forEach(collected::add);

        assertEquals(List.of(1, 2, 3), collected);
    }

    @Test
    void iteratorVariantWithoutHeadStartsFromTraverse() {
        var iterable = LinkedIterable.<String>over((String value) -> value == null, previous -> {
            if (previous == null) {
                return "a";
            }
            if ("c".equals(previous)) {
                return null;
            }
            return String.valueOf((char) (previous.charAt(0) + 1));
        });

        var values = new ArrayList<String>();
        iterable.forEach(values::add);

        assertEquals(List.of("a", "b", "c"), values);
    }

    @Test
    void spliteratorHasExpectedCharacteristics() {
        var iterable = LinkedIterable.over(1, (Integer value) -> value == null, x -> null);

        var spliterator = iterable.spliterator();

        assertEquals(LinkedIterable.SPLITERATOR_CHARACTERISTICS, spliterator.characteristics());
    }
}
