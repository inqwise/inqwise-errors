package com.inqwise.errors;

import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterator.SORTED;
import static java.util.Spliterators.spliteratorUnknownSize;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.function.Predicate;

final class LinkedIterable<T>
	   implements Iterable<T> {
    /** The characteristics for spliterators. */
    public static final int SPLITERATOR_CHARACTERISTICS = IMMUTABLE | NONNULL | ORDERED | SORTED;

    private final T head;
    private final Function<T, T> traverse;
    private final Predicate<T> terminate;

    public static <T> Iterable<T> over( final T head,
		   final Predicate<T> terminate,  final Function<T, T> traverse) {
	   return new LinkedIterable<>(head, terminate, traverse);
    }

    public static <T> Iterable<T> over( final Predicate<T> terminate,
		   final Function<T, T> traverse) {
	   return new LinkedIterable<>(traverse.apply(null), terminate, traverse);
    }

    private LinkedIterable(final T head, final Predicate<T> terminate,
		  final Function<T, T> traverse) {
	   this.head = head;
	   this.terminate = terminate;
	   this.traverse = traverse;
    }

    @Override
    public Iterator<T> iterator() {
	   return new Iterator<T>() {
		  private T last = head;

		  @Override
		  public boolean hasNext() {
			 return !terminate.test(last);
		  }

		  @Override
		  public T next() {
			 final T next = last;
			 last = traverse.apply(last);
			 return next;
		  }
	   };
    }

    @Override
    public Spliterator<T> spliterator() {
	   return spliteratorUnknownSize(iterator(), SPLITERATOR_CHARACTERISTICS);
    }
}