package net.sothatsit.function;

import net.sothatsit.property.Property;

/**
 * A class representing a value that is either one type or the other.
 *
 * @author Paddy Lamont
 */
public abstract class Either<L, R> {

    public abstract boolean isLeft();

    public boolean isRight() {
        return !isLeft();
    }

    public abstract L getLeft();

    public abstract R getRight();

    public L getLeftOrDefault(L defaultValue) {
        return (isLeft() ? getLeft() : defaultValue);
    }

    public R getRightOrDefault(R defaultValue) {
        return (isRight() ? getRight() : defaultValue);
    }

    public L getLeftOrNull() {
        return getLeftOrDefault(null);
    }

    public R getRightOrNull() {
        return getRightOrDefault(null);
    }

    public <T> Either<L, T> left() {
        return new Left<>(getLeft());
    }

    public <T> Either<T, R> right() {
        return new Right<>(getRight());
    }

    private static class Left<L, R> extends Either<L, R> {

        private final L value;

        public Left(L value) {
            this.value = value;
        }

        @Override
        public boolean isLeft() {
            return true;
        }

        @Override
        public L getLeft() {
            return value;
        }

        @Override
        public R getRight() {
            throw new UnsupportedOperationException("This is a left value");
        }
    }

    private static class Right<L, R> extends Either<L, R> {

        private final R value;

        public Right(R value) {
            this.value = value;
        }

        @Override
        public boolean isLeft() {
            return false;
        }

        @Override
        public L getLeft() {
            throw new UnsupportedOperationException("This is a right value");
        }

        @Override
        public R getRight() {
            return value;
        }
    }

    public static <L, R> Either<L, R> left(L value) {
        return new Left<>(value);
    }

    public static <L, R> Either<L, R> right(R value) {
        return new Right<>(value);
    }

    public static <L, R> Property<L> getLeftOrDefault(Property<Either<L, R>> eitherProperty, L defaultValue) {
        String name = "getLeft(" + eitherProperty.getName() + ")";
        return eitherProperty.map(name, either -> either.getLeftOrDefault(defaultValue));
    }

    public static <L, R> Property<R> getRightOrDefault(Property<Either<L, R>> eitherProperty, R defaultValue) {
        String name = "getRight(" + eitherProperty.getName() + ")";
        return eitherProperty.map(name, either -> either.getRightOrDefault(defaultValue));
    }

    public static <L, R> Property<L> getLeftOrNull(Property<Either<L, R>> eitherProperty) {
        return getLeftOrDefault(eitherProperty, null);
    }

    public static <L, R> Property<R> getRightOrNull(Property<Either<L, R>> eitherProperty) {
        return getRightOrDefault(eitherProperty, null);
    }

    public static <L, R> Property<Boolean> isLeft(Property<Either<L, R>> eitherProperty) {
        return isLeft("isLeft(" + eitherProperty.getName() + ")", eitherProperty);
    }

    public static <L, R> Property<Boolean> isLeft(String name, Property<Either<L, R>> eitherProperty) {
        return eitherProperty.map(name, Either::isLeft);
    }

    public static <L, R> Property<Boolean> isRight(Property<Either<L, R>> eitherProperty) {
        return isRight("isRight(" + eitherProperty.getName() + ")", eitherProperty);
    }

    public static <L, R> Property<Boolean> isRight(String name, Property<Either<L, R>> eitherProperty) {
        return eitherProperty.map(name, Either::isRight);
    }
}
