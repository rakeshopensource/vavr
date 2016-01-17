/*     / \____  _    _  ____   ______  / \ ____  __    _ _____
 *    /  /    \/ \  / \/    \ /  /\__\/  //    \/  \  / /  _  \   Javaslang
 *  _/  /  /\  \  \/  /  /\  \\__\\  \  //  /\  \ /\\/  \__/  /   Copyright 2014-now Daniel Dietrich
 * /___/\_/  \_/\____/\_/  \_/\__\/__/___\_/  \_//  \__/_____/    Licensed under the Apache License, Version 2.0
 */
package javaslang.control;

import javaslang.*;
import javaslang.algebra.Applicative;
import javaslang.algebra.BiFoldable;
import javaslang.algebra.BiFunctor;
import javaslang.algebra.Kind2;
import javaslang.collection.Iterator;
import javaslang.collection.List;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * An implementation similar to scalaz's <a href="http://eed3si9n.com/learning-scalaz/Validation.html">Validation</a> control.
 *
 * <p>
 * The Validation type is different from a Monad type, it is an applicative
 * functor. Whereas a Monad will short circuit after the first error, the
 * applicative functor will continue on, accumulating ALL errors. This is
 * especially helpful in cases such as validation, where you want to know
 * all the validation errors that have occurred, not just the first one.
 * </p>
 *
 * <pre>
 * <code>
 * <b>Validation construction:</b>
 *
 * <i>Valid:</i>
 * Validation&lt;String,Integer&gt; valid = Validation.valid(5);
 *
 * <i>Invalid:</i>
 * Validation&lt;List&lt;String&gt;,Integer&gt; invalid = Validation.invalid(List.of("error1","error2"));
 *
 * <b>Validation combination:</b>
 *
 * Validation&lt;String,String&gt; valid1 = Validation.valid("John");
 * Validation&lt;String,Integer&gt; valid2 = Validation.valid(5);
 * Validation&lt;String,Option&lt;String&gt;&gt; valid3 = Validation.valid(Option.of("123 Fake St."));
 * Function3&lt;String,Integer,Option&lt;String&gt;,Person&gt; f = ...;
 *
 * Validation&lt;List&lt;String&gt;,String&gt; result = valid1.combine(valid2).ap((name,age) -&gt; "Name: "+name+" Age: "+age);
 * Validation&lt;List&lt;String&gt;,Person&gt; result2 = valid1.combine(valid2).combine(valid3).ap(f);
 * </code>
 * </pre>
 *
 * @param <E> value type in the case of invalid
 * @param <T> value type in the case of valid
 * @author Eric Nelson
 * @see <a href="https://github.com/scalaz/scalaz/blob/series/7.3.x/core/src/main/scala/scalaz/Validation.scala">Validation</a>
 * @since 2.0.0
 */
public interface Validation<E, T> extends Value<T>, Applicative<Validation<?, ?>, E, T>, BiFunctor<E, T>, BiFoldable<E, T> {

    /**
     * Creates a {@link Valid} that contains the given {@code value}.
     *
     * @param <E>   type of the error
     * @param <T>   type of the given {@code value}
     * @param value A value
     * @return {@code Valid(value)}
     * @throws NullPointerException if value is null
     */
    static <E, T> Validation<E, T> valid(T value) {
        Objects.requireNonNull(value, "value is null");
        return new Valid<>(value);
    }

    /**
     * Creates an {@link Invalid} that contains the given {@code error}.
     *
     * @param <E>   type of the given {@code error}
     * @param <T>   type of the value
     * @param error An error
     * @return {@code Invalid(error)}
     * @throws NullPointerException if error is null
     */
    static <E, T> Validation<E, T> invalid(E error) {
        Objects.requireNonNull(error, "error is null");
        return new Invalid<>(error);
    }

    /**
     * Creates a {@code Validation} of an {@code Either}.
     *
     * @param either An {@code Either}
     * @param <E>    type of the given {@code error}
     * @param <T>    type of the value
     * @return A {@code Valid(either.get())} if either is a Right, otherwise {@code Invalid(either.getLeft())}.
     * @throws NullPointerException if either is null
     */
    static <E, T> Validation<E, T> from(Either<E, T> either) {
        Objects.requireNonNull(either, "either is null");
        return either.isRight() ? valid(either.get()) : invalid(either.getLeft());
    }

    /**
     * Combines two {@code Validation}s into a {@link Builder}.
     *
     * @param <E>         type of error
     * @param <T1>        type of first valid value
     * @param <T2>        type of second valid value
     * @param validation1 first validation
     * @param validation2 second validation
     * @return an instance of Builder&lt;E,T1,T2&gt;
     * @throws NullPointerException if validation1 or validation2 is null
     */
    static <E, T1, T2> Builder<E, T1, T2> combine(Validation<E, T1> validation1, Validation<E, T2> validation2) {
        Objects.requireNonNull(validation1, "validation1 is null");
        Objects.requireNonNull(validation2, "validation2 is null");
        return new Builder<>(validation1, validation2);
    }

    /**
     * Combines three {@code Validation}s into a {@link Builder3}.
     *
     * @param <E>         type of error
     * @param <T1>        type of first valid value
     * @param <T2>        type of second valid value
     * @param <T3>        type of third valid value
     * @param validation1 first validation
     * @param validation2 second validation
     * @param validation3 third validation
     * @return an instance of Builder3&lt;E,T1,T2,T3&gt;
     * @throws NullPointerException if validation1, validation2 or validation3 is null
     */
    static <E, T1, T2, T3> Builder3<E, T1, T2, T3> combine(Validation<E, T1> validation1, Validation<E, T2> validation2, Validation<E, T3> validation3) {
        Objects.requireNonNull(validation1, "validation1 is null");
        Objects.requireNonNull(validation2, "validation2 is null");
        Objects.requireNonNull(validation3, "validation3 is null");
        return new Builder3<>(validation1, validation2, validation3);
    }

    /**
     * Combines four {@code Validation}s into a {@link Builder4}.
     *
     * @param <E>         type of error
     * @param <T1>        type of first valid value
     * @param <T2>        type of second valid value
     * @param <T3>        type of third valid value
     * @param <T4>        type of fourth valid value
     * @param validation1 first validation
     * @param validation2 second validation
     * @param validation3 third validation
     * @param validation4 fourth validation
     * @return an instance of Builder3&lt;E,T1,T2,T3,T4&gt;
     * @throws NullPointerException if validation1, validation2, validation3 or validation4 is null
     */
    static <E, T1, T2, T3, T4> Builder4<E, T1, T2, T3, T4> combine(Validation<E, T1> validation1, Validation<E, T2> validation2, Validation<E, T3> validation3, Validation<E, T4> validation4) {
        Objects.requireNonNull(validation1, "validation1 is null");
        Objects.requireNonNull(validation2, "validation2 is null");
        Objects.requireNonNull(validation3, "validation3 is null");
        Objects.requireNonNull(validation4, "validation4 is null");
        return new Builder4<>(validation1, validation2, validation3, validation4);
    }

    /**
     * Combines five {@code Validation}s into a {@link Builder5}.
     *
     * @param <E>         type of error
     * @param <T1>        type of first valid value
     * @param <T2>        type of second valid value
     * @param <T3>        type of third valid value
     * @param <T4>        type of fourth valid value
     * @param <T5>        type of fifth valid value
     * @param validation1 first validation
     * @param validation2 second validation
     * @param validation3 third validation
     * @param validation4 fourth validation
     * @param validation5 fifth validation
     * @return an instance of Builder3&lt;E,T1,T2,T3,T4,T5&gt;
     * @throws NullPointerException if validation1, validation2, validation3, validation4 or validation5 is null
     */
    static <E, T1, T2, T3, T4, T5> Builder5<E, T1, T2, T3, T4, T5> combine(Validation<E, T1> validation1, Validation<E, T2> validation2, Validation<E, T3> validation3, Validation<E, T4> validation4, Validation<E, T5> validation5) {
        Objects.requireNonNull(validation1, "validation1 is null");
        Objects.requireNonNull(validation2, "validation2 is null");
        Objects.requireNonNull(validation3, "validation3 is null");
        Objects.requireNonNull(validation4, "validation4 is null");
        Objects.requireNonNull(validation5, "validation5 is null");
        return new Builder5<>(validation1, validation2, validation3, validation4, validation5);
    }

    /**
     * Combines six {@code Validation}s into a {@link Builder6}.
     *
     * @param <E>         type of error
     * @param <T1>        type of first valid value
     * @param <T2>        type of second valid value
     * @param <T3>        type of third valid value
     * @param <T4>        type of fourth valid value
     * @param <T5>        type of fifth valid value
     * @param <T6>        type of sixth valid value
     * @param validation1 first validation
     * @param validation2 second validation
     * @param validation3 third validation
     * @param validation4 fourth validation
     * @param validation5 fifth validation
     * @param validation6 sixth validation
     * @return an instance of Builder3&lt;E,T1,T2,T3,T4,T5,T6&gt;
     * @throws NullPointerException if validation1, validation2, validation3, validation4, validation5 or validation6 is null
     */
    static <E, T1, T2, T3, T4, T5, T6> Builder6<E, T1, T2, T3, T4, T5, T6> combine(Validation<E, T1> validation1, Validation<E, T2> validation2, Validation<E, T3> validation3, Validation<E, T4> validation4, Validation<E, T5> validation5, Validation<E, T6> validation6) {
        Objects.requireNonNull(validation1, "validation1 is null");
        Objects.requireNonNull(validation2, "validation2 is null");
        Objects.requireNonNull(validation3, "validation3 is null");
        Objects.requireNonNull(validation4, "validation4 is null");
        Objects.requireNonNull(validation5, "validation5 is null");
        Objects.requireNonNull(validation6, "validation6 is null");
        return new Builder6<>(validation1, validation2, validation3, validation4, validation5, validation6);
    }

    /**
     * Combines seven {@code Validation}s into a {@link Builder7}.
     *
     * @param <E>         type of error
     * @param <T1>        type of first valid value
     * @param <T2>        type of second valid value
     * @param <T3>        type of third valid value
     * @param <T4>        type of fourth valid value
     * @param <T5>        type of fifth valid value
     * @param <T6>        type of sixth valid value
     * @param <T7>        type of seventh valid value
     * @param validation1 first validation
     * @param validation2 second validation
     * @param validation3 third validation
     * @param validation4 fourth validation
     * @param validation5 fifth validation
     * @param validation6 sixth validation
     * @param validation7 seventh validation
     * @return an instance of Builder3&lt;E,T1,T2,T3,T4,T5,T6,T7&gt;
     * @throws NullPointerException if validation1, validation2, validation3, validation4, validation5, validation6 or validation7 is null
     */
    static <E, T1, T2, T3, T4, T5, T6, T7> Builder7<E, T1, T2, T3, T4, T5, T6, T7> combine(Validation<E, T1> validation1, Validation<E, T2> validation2, Validation<E, T3> validation3, Validation<E, T4> validation4, Validation<E, T5> validation5, Validation<E, T6> validation6, Validation<E, T7> validation7) {
        Objects.requireNonNull(validation1, "validation1 is null");
        Objects.requireNonNull(validation2, "validation2 is null");
        Objects.requireNonNull(validation3, "validation3 is null");
        Objects.requireNonNull(validation4, "validation4 is null");
        Objects.requireNonNull(validation5, "validation5 is null");
        Objects.requireNonNull(validation6, "validation6 is null");
        Objects.requireNonNull(validation7, "validation7 is null");
        return new Builder7<>(validation1, validation2, validation3, validation4, validation5, validation6, validation7);
    }

    /**
     * Combines eight {@code Validation}s into a {@link Builder8}.
     *
     * @param <E>         type of error
     * @param <T1>        type of first valid value
     * @param <T2>        type of second valid value
     * @param <T3>        type of third valid value
     * @param <T4>        type of fourth valid value
     * @param <T5>        type of fifth valid value
     * @param <T6>        type of sixth valid value
     * @param <T7>        type of seventh valid value
     * @param <T8>        type of eighth valid value
     * @param validation1 first validation
     * @param validation2 second validation
     * @param validation3 third validation
     * @param validation4 fourth validation
     * @param validation5 fifth validation
     * @param validation6 sixth validation
     * @param validation7 seventh validation
     * @param validation8 eigth validation
     * @return an instance of Builder3&lt;E,T1,T2,T3,T4,T5,T6,T7,T8&gt;
     * @throws NullPointerException if validation1, validation2, validation3, validation4, validation5, validation6, validation7 or validation8 is null
     */
    static <E, T1, T2, T3, T4, T5, T6, T7, T8> Builder8<E, T1, T2, T3, T4, T5, T6, T7, T8> combine(Validation<E, T1> validation1, Validation<E, T2> validation2, Validation<E, T3> validation3, Validation<E, T4> validation4, Validation<E, T5> validation5, Validation<E, T6> validation6, Validation<E, T7> validation7, Validation<E, T8> validation8) {
        Objects.requireNonNull(validation1, "validation1 is null");
        Objects.requireNonNull(validation2, "validation2 is null");
        Objects.requireNonNull(validation3, "validation3 is null");
        Objects.requireNonNull(validation4, "validation4 is null");
        Objects.requireNonNull(validation5, "validation5 is null");
        Objects.requireNonNull(validation6, "validation6 is null");
        Objects.requireNonNull(validation7, "validation7 is null");
        Objects.requireNonNull(validation8, "validation8 is null");
        return new Builder8<>(validation1, validation2, validation3, validation4, validation5, validation6, validation7, validation8);
    }

    /**
     * Check whether this is of type {@code Valid}
     *
     * @return true if is a Valid, false if is an Invalid
     */
    boolean isValid();

    /**
     * Check whether this is of type {@code Invalid}
     *
     * @return true if is an Invalid, false if is a Valid
     */
    boolean isInvalid();

    @Override
    default boolean isEmpty() {
        return isInvalid();
    }

    /**
     * Gets the value of this Validation if is a Valid or throws if this is an Invalid
     *
     * @return The value of this Validation
     * @throws NoSuchElementException if this is an Invalid
     */
    @Override
    T get();

    /**
     * Gets the error of this Validation if is an Invalid or throws if this is a Valid
     *
     * @return The error of this Invalid
     * @throws RuntimeException if this is a Valid
     */
    E getError();

    /**
     * Returns this as {@code Either}.
     *
     * @return {@code Either.right(get())} if this is valid, otherwise {@code Either.left(getError())}.
     */
    default Either<E, T> toEither() {
        return isValid() ? Either.right(get()) : Either.left(getError());
    }

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();

    @Override
    String toString();

    /**
     * Performs the given action for the value contained in {@code Valid}, or do nothing
     * if this is an Invalid.
     *
     * @param action the action to be performed on the contained value
     * @throws NullPointerException if action is null
     */
    @Override
    default void forEach(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action is null");
        if (isValid()) {
            action.accept(get());
        }
    }

    /**
     * Performs the action in fInvalid on error if this is an Invalid, or fValid on value if
     * this is a Valid. Returns an object of type U.
     *
     * <p>
     * <code>
     * For example:<br>
     * Validation&lt;List&lt;String&gt;,String&gt; valid = ...;<br>
     * Integer i = valid.fold(List::length, String::length);
     * </code>
     * </p>
     *
     * @param <U>      the fold result type
     * @param fInvalid the invalid fold operation
     * @param fValid   the valid fold operation
     * @return an instance of type U
     * @throws NullPointerException if fInvalid or fValid is null
     */
    @Override
    default <U> U bifold(Function<? super E, ? extends U> fInvalid, Function<? super T, ? extends U> fValid) {
        Objects.requireNonNull(fInvalid, "function fInvalid null");
        Objects.requireNonNull(fValid, "function fValid null");
        if (isInvalid()) {
            E error = this.getError();
            return fInvalid.apply(error);
        } else {
            T value = this.get();
            return fValid.apply(value);
        }
    }

    /**
     * Flip the valid/invalid values for this Validation. If this is a Valid&lt;E,T&gt;, returns Invalid&lt;T,E&gt;.
     * Or if this is an Invalid&lt;E,T&gt;, return a Valid&lt;T,E&gt;.
     *
     * @return a flipped instance of Validation
     */
    default Validation<T, E> swap() {
        if (isInvalid()) {
            E error = this.getError();
            return Validation.valid(error);
        } else {
            T value = this.get();
            return Validation.invalid(value);
        }
    }

    @Override
    default <U> Validation<E, U> map(Function<? super T, ? extends U> f) {
        Objects.requireNonNull(f, "function f is null");
        if (isInvalid()) {
            return Validation.invalid(this.getError());
        } else {
            T value = this.get();
            return Validation.valid(f.apply(value));
        }
    }

    /**
     * Whereas map only performs a mapping on a valid Validation, and leftMap performs a mapping on an invalid
     * Validation, bimap allows you to provide mapping actions for both, and will give you the result based
     * on what type of Validation this is. Without this, you would have to do something like:
     *
     * validation.map(...).leftMap(...);
     *
     * @param <U>           type of the mapping result if this is an invalid
     * @param <R>           type of the mapping result if this is a valid
     * @param invalidMapper the invalid mapping operation
     * @param validMapper   the valid mapping operation
     * @return an instance of Validation&lt;U,R&gt;
     * @throws NullPointerException if invalidMapper or validMapper is null
     */
    @Override
    default <U, R> Validation<U, R> bimap(Function<? super E, ? extends U> invalidMapper, Function<? super T, ? extends R> validMapper) {
        Objects.requireNonNull(invalidMapper, "function invalidMapper is null");
        Objects.requireNonNull(validMapper, "function validMapper is null");
        if (isInvalid()) {
            E error = this.getError();
            return Validation.invalid(invalidMapper.apply(error));
        } else {
            T value = this.get();
            return Validation.valid(validMapper.apply(value));
        }
    }

    /**
     * Applies a function f to the error of this Validation if this is an Invalid. Otherwise does nothing
     * if this is a Valid.
     *
     * @param <U> type of the error resulting from the mapping
     * @param f   a function that maps the error in this Invalid
     * @return an instance of Validation&lt;U,T&gt;
     * @throws NullPointerException if mapping operation f is null
     */
    default <U> Validation<U, T> leftMap(Function<? super E, ? extends U> f) {
        Objects.requireNonNull(f, "function f is null");
        if (isInvalid()) {
            E error = this.getError();
            return Validation.invalid(f.apply(error));
        } else {
            return Validation.valid(this.get());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    default <U> Validation<List<E>, U> ap(Kind2<Validation<?, ?>, List<E>, ? extends Function<? super T, ? extends U>> kind) {
        Objects.requireNonNull(kind, "kind is null");
        Validation<List<E>, Function<T, U>> validation = (Validation<List<E>, Function<T, U>>) (Object) kind;

        if (isValid() && validation.isValid()) {
            Function<? super T, ? extends U> f = validation.get();
            U u = f.apply(this.get());
            return valid(u);
        } else if (isValid() && validation.isInvalid()) {
            List<E> errors = validation.getError();
            return invalid(errors);
        } else if (isInvalid() && validation.isValid()) {
            E error = this.getError();
            return invalid(List.of(error));
        } else {
            List<E> errors = validation.getError();
            E error = this.getError();
            return invalid(errors.append(error));
        }
    }

    /**
     * Combines two {@code Validation}s to form a {@link Builder}, which can then be used to perform further
     * combines, or apply a function to it in order to transform the {@link Builder} into a {@code Validation}.
     *
     * @param <U>        type of the value contained in validation
     * @param validation the validation object to combine this with
     * @return an instance of Builder
     */
    default <U> Builder<E, T, U> combine(Validation<E, U> validation) {
        return new Builder<>(this, validation);
    }

    // -- Implementation of Value

    @Override
    default Option<Validation<E, T>> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return (isEmpty() || predicate.test(get())) ? Option.some(this) : Option.none();
    }

    @Override
    default Option<Validation<E, T>> filterNot(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return filter(predicate.negate());
    }

    @SuppressWarnings("unchecked")
    @Override
    default <U> Validation<E, U> flatMap(Function<? super T, ? extends Iterable<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        if (isEmpty()) {
            return (Validation<E, U>) this;
        } else {
            final Iterable<? extends U> iterable = mapper.apply(get());
            if (iterable instanceof Validation) {
                return (Validation<E, U>) iterable;
            } else if (iterable instanceof Value) {
                final Value<U> value = (Value<U>) iterable;
                return value.isEmpty() ? /*TODO(#1034)*/invalid(null) : valid(value.get());
            } else {
                final java.util.Iterator<? extends U> iterator = iterable.iterator();
                return iterator.hasNext() ? /*TODO(#1034)*/invalid(null) : valid(iterator.next());
            }
        }
    }


    @Override
    default Match.MatchMonad.Of<Validation<E, T>> match() {
        return Match.of(this);
    }

    @Override
    default Validation<E, T> peek(Consumer<? super T> action) {
        if (isValid()) {
            action.accept(get());
        }
        return this;
    }

    @Override
    default boolean isSingleValued() {
        return true;
    }

    @Override
    default Iterator<T> iterator() {
        return isValid() ? Iterator.of(get()) : Iterator.empty();
    }

    /**
     * A valid Validation
     *
     * @param <E> type of the error of this Validation
     * @param <T> type of the value of this Validation
     */
    final class Valid<E, T> implements Validation<E, T> {

        private final T value;

        /**
         * Construct a {@code Valid}
         *
         * @param value The value of this success
         */
        private Valid(T value) {
            this.value = value;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public boolean isInvalid() {
            return false;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public E getError() throws RuntimeException {
            throw new NoSuchElementException("error of 'valid' Validation");
        }

        @Override
        public boolean equals(Object obj) {
            return (obj == this) || (obj instanceof Valid && Objects.equals(value, ((Valid<?, ?>) obj).value));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public String stringPrefix() {
            return "Valid";
        }

        @Override
        public String toString() {
            return stringPrefix() + "(" + value + ")";
        }

    }

    /**
     * An invalid Validation
     *
     * @param <E> type of the error of this Validation
     * @param <T> type of the value of this Validation
     */
    final class Invalid<E, T> implements Validation<E, T> {

        private final E error;

        /**
         * Construct an {@code Invalid}
         *
         * @param error The value of this error
         */
        private Invalid(E error) {
            this.error = error;
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public boolean isInvalid() {
            return true;
        }

        @Override
        public T get() throws RuntimeException {
            throw new NoSuchElementException("get of 'invalid' Validation");
        }

        @Override
        public E getError() {
            return error;
        }

        @Override
        public boolean equals(Object obj) {
            return (obj == this) || (obj instanceof Invalid && Objects.equals(error, ((Invalid<?, ?>) obj).error));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(error);
        }

        @Override
        public String stringPrefix() {
            return "Invalid";
        }

        @Override
        public String toString() {
            return stringPrefix() + "(" + error + ")";
        }

    }

    final class Builder<E, T1, T2> {

        private Validation<E, T1> v1;
        private Validation<E, T2> v2;

        private Builder(Validation<E, T1> v1, Validation<E, T2> v2) {
            this.v1 = v1;
            this.v2 = v2;
        }

        public <R> Validation<List<E>, R> ap(Function2<T1, T2, R> f) {
            return v2.ap(v1.ap(Validation.valid(f.curried())));
        }

        public <T3> Builder3<E, T1, T2, T3> combine(Validation<E, T3> v3) {
            return new Builder3<>(v1, v2, v3);
        }

    }

    final class Builder3<E, T1, T2, T3> {

        private Validation<E, T1> v1;
        private Validation<E, T2> v2;
        private Validation<E, T3> v3;

        private Builder3(Validation<E, T1> v1, Validation<E, T2> v2, Validation<E, T3> v3) {
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
        }

        public <R> Validation<List<E>, R> ap(Function3<T1, T2, T3, R> f) {
            return v3.ap(v2.ap(v1.ap(Validation.valid(f.curried()))));
        }

        public <T4> Builder4<E, T1, T2, T3, T4> combine(Validation<E, T4> v4) {
            return new Builder4<>(v1, v2, v3, v4);
        }

    }

    final class Builder4<E, T1, T2, T3, T4> {

        private Validation<E, T1> v1;
        private Validation<E, T2> v2;
        private Validation<E, T3> v3;
        private Validation<E, T4> v4;

        private Builder4(Validation<E, T1> v1, Validation<E, T2> v2, Validation<E, T3> v3, Validation<E, T4> v4) {
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
            this.v4 = v4;
        }

        public <R> Validation<List<E>, R> ap(Function4<T1, T2, T3, T4, R> f) {
            return v4.ap(v3.ap(v2.ap(v1.ap(Validation.valid(f.curried())))));
        }

        public <T5> Builder5<E, T1, T2, T3, T4, T5> combine(Validation<E, T5> v5) {
            return new Builder5<>(v1, v2, v3, v4, v5);
        }

    }

    final class Builder5<E, T1, T2, T3, T4, T5> {

        private Validation<E, T1> v1;
        private Validation<E, T2> v2;
        private Validation<E, T3> v3;
        private Validation<E, T4> v4;
        private Validation<E, T5> v5;

        private Builder5(Validation<E, T1> v1, Validation<E, T2> v2, Validation<E, T3> v3, Validation<E, T4> v4, Validation<E, T5> v5) {
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
            this.v4 = v4;
            this.v5 = v5;
        }

        public <R> Validation<List<E>, R> ap(Function5<T1, T2, T3, T4, T5, R> f) {
            return v5.ap(v4.ap(v3.ap(v2.ap(v1.ap(Validation.valid(f.curried()))))));
        }

        public <T6> Builder6<E, T1, T2, T3, T4, T5, T6> combine(Validation<E, T6> v6) {
            return new Builder6<>(v1, v2, v3, v4, v5, v6);
        }

    }

    final class Builder6<E, T1, T2, T3, T4, T5, T6> {

        private Validation<E, T1> v1;
        private Validation<E, T2> v2;
        private Validation<E, T3> v3;
        private Validation<E, T4> v4;
        private Validation<E, T5> v5;
        private Validation<E, T6> v6;

        private Builder6(Validation<E, T1> v1, Validation<E, T2> v2, Validation<E, T3> v3, Validation<E, T4> v4, Validation<E, T5> v5, Validation<E, T6> v6) {
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
            this.v4 = v4;
            this.v5 = v5;
            this.v6 = v6;
        }

        public <R> Validation<List<E>, R> ap(Function6<T1, T2, T3, T4, T5, T6, R> f) {
            return v6.ap(v5.ap(v4.ap(v3.ap(v2.ap(v1.ap(Validation.valid(f.curried())))))));
        }

        public <T7> Builder7<E, T1, T2, T3, T4, T5, T6, T7> combine(Validation<E, T7> v7) {
            return new Builder7<>(v1, v2, v3, v4, v5, v6, v7);
        }

    }

    final class Builder7<E, T1, T2, T3, T4, T5, T6, T7> {

        private Validation<E, T1> v1;
        private Validation<E, T2> v2;
        private Validation<E, T3> v3;
        private Validation<E, T4> v4;
        private Validation<E, T5> v5;
        private Validation<E, T6> v6;
        private Validation<E, T7> v7;

        private Builder7(Validation<E, T1> v1, Validation<E, T2> v2, Validation<E, T3> v3, Validation<E, T4> v4, Validation<E, T5> v5, Validation<E, T6> v6, Validation<E, T7> v7) {
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
            this.v4 = v4;
            this.v5 = v5;
            this.v6 = v6;
            this.v7 = v7;
        }

        public <R> Validation<List<E>, R> ap(Function7<T1, T2, T3, T4, T5, T6, T7, R> f) {
            return v7.ap(v6.ap(v5.ap(v4.ap(v3.ap(v2.ap(v1.ap(Validation.valid(f.curried()))))))));
        }

        public <T8> Builder8<E, T1, T2, T3, T4, T5, T6, T7, T8> combine(Validation<E, T8> v8) {
            return new Builder8<>(v1, v2, v3, v4, v5, v6, v7, v8);
        }

    }

    final class Builder8<E, T1, T2, T3, T4, T5, T6, T7, T8> {

        private Validation<E, T1> v1;
        private Validation<E, T2> v2;
        private Validation<E, T3> v3;
        private Validation<E, T4> v4;
        private Validation<E, T5> v5;
        private Validation<E, T6> v6;
        private Validation<E, T7> v7;
        private Validation<E, T8> v8;

        private Builder8(Validation<E, T1> v1, Validation<E, T2> v2, Validation<E, T3> v3, Validation<E, T4> v4, Validation<E, T5> v5, Validation<E, T6> v6, Validation<E, T7> v7, Validation<E, T8> v8) {
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
            this.v4 = v4;
            this.v5 = v5;
            this.v6 = v6;
            this.v7 = v7;
            this.v8 = v8;
        }

        public <R> Validation<List<E>, R> ap(Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> f) {
            return v8.ap(v7.ap(v6.ap(v5.ap(v4.ap(v3.ap(v2.ap(v1.ap(Validation.valid(f.curried())))))))));
        }
    }
}
