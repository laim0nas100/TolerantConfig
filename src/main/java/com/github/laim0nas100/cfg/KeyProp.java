package com.github.laim0nas100.cfg;

import com.github.laim0nas100.cfg.TolerantConfig.ConversionTolerantFunction;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Facilitates static, typed, mapped, cached properties. Parameter indirection.
 *
 * @author laim0nas100
 */
public abstract class KeyProp {

    public static <T> Builder<T> ofAny(String key) {
        return new Builder<>(key, f -> f.getAny(key));
    }

    public static Builder<Boolean> ofBoolean(String key) {
        return new Builder<>(key, f -> f.getBoolean(key));
    }

    public static Builder<String> ofString(String key) {
        return new Builder<>(key, f -> f.getString(key));
    }

    public static Builder<Integer> ofInteger(String key) {
        return new Builder<>(key, f -> f.getInt(key));
    }

    public static Builder<Long> ofLong(String key) {
        return new Builder<>(key, f -> f.getLong(key));
    }

    public static Builder<Float> ofFloat(String key) {
        return new Builder<>(key, f -> f.getFloat(key));
    }

    public static Builder<Double> ofDouble(String key) {
        return new Builder<>(key, f -> f.getDouble(key));
    }

    public static Builder<BigInteger> ofBigInteger(String key) {
        return new Builder<>(key, f -> f.getBigInteger(key));
    }

    public static Builder<BigDecimal> ofBigDecimal(String key) {
        return new Builder<>(key, f -> f.getBigDecimal(key));
    }

    public static Builder<String[]> ofStringArray(String key) {
        return new Builder<>(key, f -> f.getStringArray(key));
    }

    public static Builder<List<String>> ofStringList(String key) {
        return new Builder<>(key, f -> f.getList(key));
    }

    public static <C extends Enum<C>> Builder<C> ofEnum(String key, Class<C> type) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(type);
        return new Builder<>(key, c -> c.getEnum(key, type));
    }

    public static <C extends Enum<C>> Builder<C> ofEnum(String key, C defaultEnum) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(defaultEnum);
        return new Builder<>(key, c -> c.getEnum(key, defaultEnum));
    }

    public static abstract class BuilderShared<T> {

        protected String key;
        protected boolean cachable;
        protected ConversionTolerantFunction< ? extends T> fun;

        protected BuilderShared(String key, ConversionTolerantFunction< ? extends T> fun) {
            this.key = Objects.requireNonNull(key);
            this.fun = Objects.requireNonNull(fun);
            this.cachable = false;
        }

        protected BuilderShared(BuilderShared prev) {
            this.cachable = prev.cachable;
            this.key = prev.key;
            this.fun = prev.fun;
        }

        protected ConversionTolerantFunction getFinalFunction() {
            return fun;
        }

        public Key<T> toKey() {
            return () -> key;
        }

        public ResolvableKeyProperty<T> toKeyProperty() {
            return new ResolvableKeyProperty<>(cachable, key, getFinalFunction());
        }

        public PreparedProp<T> toPreparedProperty(Supplier<List<TolerantConfig>> prepared) {
            return new PreparedProp<>(prepared, cachable, key, getFinalFunction());
        }

        public KeyDefaultProperty<T> toKeyDefaultProperty(T defaultVal) {
            return new ResolvableDefaultKeyProperty<>(cachable, key, defaultVal, getFinalFunction());
        }

        public PreparedDefaultProp<T> toPreparedKeyDefaultProperty(T defaultVal, Supplier<List<TolerantConfig>> prepared) {
            return new PreparedDefaultProp<>(prepared, cachable, key, defaultVal, getFinalFunction());
        }

    }

    public static class MappedBuilder<S, R> extends BuilderShared<R> {

        protected final Function<S, ? extends R> mapper;

        public MappedBuilder(BuilderShared previous, Function<S, ? extends R> mapper) {
            super(previous);
            this.mapper = Objects.requireNonNull(mapper);
        }

        public MappedBuilder(MappedBuilder previous) {
            super(previous);
            this.mapper = previous.mapper;
        }

        @Override
        protected ConversionTolerantFunction<? extends R> getFinalFunction() {
            final ConversionTolerantFunction<? extends S> original = super.getFinalFunction();
            return new ConversionTolerantFunction<R>() {
                @Override
                public R convert(TolerantConfig t) throws Exception {
                    S source = original.convert(t);
                    return mapper.apply(source);
                }
            };
        }

        public <A> MappedBuilder<S, A> map(Function<R, ? extends A> function) {
            return new MappedBuilder<>(this, mapper.andThen(function));
        }

        public MappedBuilder<S, R> cache(boolean cachable) {
            this.cachable = cachable;
            return this;
        }

        @Override
        public PreparedDefaultProp<R> toPreparedKeyDefaultProperty(R defaultVal, Supplier<List<TolerantConfig>> prepared) {
            return super.toPreparedKeyDefaultProperty(defaultVal, prepared);
        }

        @Override
        public KeyDefaultProperty<R> toKeyDefaultProperty(R defaultVal) {
            return super.toKeyDefaultProperty(defaultVal);
        }

        @Override
        public PreparedProp<R> toPreparedProperty(Supplier<List<TolerantConfig>> prepared) {
            return super.toPreparedProperty(prepared);
        }

        @Override
        public ResolvableKeyProperty<R> toKeyProperty() {
            return super.toKeyProperty();
        }

        @Override
        public Key<R> toKey() {
            return super.toKey();
        }

    }

    public static class Builder<T> extends BuilderShared<T> {

        protected Builder(String key, ConversionTolerantFunction< ? extends T> fun) {
            super(key, fun);
        }

        protected Builder(Builder old) {
            super(old);
        }

        public <A> MappedBuilder<T, A> map(Function<T, ? extends A> function) {
            return new MappedBuilder<>(this, function);
        }

        public Builder<T> cache(boolean cachable) {
            this.cachable = cachable;
            return this;
        }

    }

    /**
     * Static String key
     *
     * @param <T>
     */
    public static interface Key<T> {

        /**
         *
         * @return key
         */
        public String getKey();
    }

    /**
     * Basically like an entry with String keys.
     *
     * @param <T>
     */
    public static interface KeyVal<T> extends Key<T> {

        /**
         *
         * @return value
         */
        public T getValue();

    }

    /**
     * Property that can resolve values
     *
     * @param <T>
     */
    public static interface KeyProperty<T> extends Key<T> {

        /**
         * Return the first resolved value based on this KeyProperty behavior
         * from the supplied TolerantConfig array. The default entry point.
         * Usually redirects to {@link KeyProperty#resolveLogic(com.github.laim0nas100.cfg.TolerantConfig...)
         * } unless there is some special behavior like caching.
         *
         * @param config
         * @return
         */
        public default T resolve(TolerantConfig... config) {
            return resolveLogic(config);
        }

        /**
         * Should not call directly. Used for separating caching logic and
         * similar.
         *
         * @param config
         * @return
         */
        public T resolveLogic(TolerantConfig... config);

        /**
         * Return the first resolved value associated with this KeyProperty from
         * the supplied TolerantConfig array
         *
         * @param config
         * @return
         */
        public default T resolveThrowIfNull(TolerantConfig... config) {
            T resolve = resolveLogic(config);
            if (resolve == null) {
                throw new NoSuchElementException(getKey() + " resolves to a null");
            }
            return resolve;
        }

        /**
         * Return the resolved value associated with this KeyProperty from the
         * supplied TolerantConfig
         *
         * @param config
         * @return
         */
        public T explicitResolve(TolerantConfig config) throws Exception;

        /**
         * Return the resolved value associated with this KeyProperty from the
         * supplied TolerantConfig or null if failed.
         *
         * @param config
         * @return
         */
        public T tolerantResolve(TolerantConfig config);

    }

    /**
     * Property that can resolve values and also has a default value
     *
     * @param <T>
     */
    public static interface KeyDefaultProperty<T> extends KeyProperty<T> {

        /**
         * The default value, if not found in any TolerantConfig
         *
         * @return
         */
        public T getDefault();

    }

    public static class Cached<T> {

        public static int MAX_CACHE_SIZE = 8;
        // FIFO cache
        protected Map<TolerantConfig, T> cached = new LinkedHashMap<>(MAX_CACHE_SIZE + 2);
        protected boolean isEmptyCached;
        protected T emptyCached; // only relevant if config returns default value that can be mapped

        private final Object lock = new Object();

        protected final KeyProperty<T> property;

        public Cached(KeyProperty<T> property) {
            this.property = property;
        }

        /**
         *
         * @param property should not change
         * @param prop
         * @return
         */
        public T resolveCached(TolerantConfig... prop) {

            if (prop.length == 0 && isEmptyCached) {
                return emptyCached;
            }
            for (TolerantConfig prop1 : prop) {
                if (cached.containsKey(prop1)) {
                    return cached.get(prop1);
                }
            }

            synchronized (lock) {
                T resolve = property.resolveLogic(prop);
                if (prop.length == 0) {
                    isEmptyCached = true;
                    emptyCached = resolve;
                } else {
                    if (prop.length == 1) {
                        cached.put(prop[0], resolve);
                    } else {
                        for (int i = 0; i < prop.length; i++) {
                            if (prop[i].containsKey(property.getKey())) {// resolve again, hopefully types match
                                if (Objects.equals(property.resolveLogic(prop[i]), resolve)) {
                                    cached.put(prop[i], resolve);
                                    break;
                                }
                            }
                        }
                    }
                    while (cached.size() > MAX_CACHE_SIZE) {
                        cached.remove(cached.keySet().iterator().next());
                    }
                }
                return resolve;
            }

        }

    }

    public static class ResolvableKeyProperty<T> implements KeyProperty<T> {

        protected final String key;
        protected final ConversionTolerantFunction< ? extends T> fun;
        protected final Cached<T> cache;

        public ResolvableKeyProperty(boolean cache, String key, ConversionTolerantFunction<? extends T> fun) {
            this.key = Objects.requireNonNull(key);
            this.fun = Objects.requireNonNull(fun);
            this.cache = cache ? new Cached<>(this) : null;
        }

        @Override
        public T resolve(TolerantConfig... config) {
            if (cache != null) {
                return cache.resolveCached(config);
            }
            return resolveLogic(config);
        }

        @Override
        public T resolveLogic(TolerantConfig... config) {

            if (config == null || config.length == 0) {
                throw new IllegalArgumentException("No config was provided");
            }
            for (int i = 0; i < config.length; i++) {
                TolerantConfig conf = config[i];
                if (conf == null) {
                    throw new IllegalArgumentException("TolerantConfig at index " + i + " is null");
                }
                if (conf.containsKey(key)) {
                    T resolved = tolerantResolve(conf);
                    if (resolved != null) {
                        return resolved;
                    }
                }

            }
            return null;
        }

        @Override
        public T explicitResolve(TolerantConfig config) throws Exception {
            return fun.convert(Objects.requireNonNull(config));
        }

        @Override
        public T tolerantResolve(TolerantConfig config) {
            return fun.apply(Objects.requireNonNull(config));
        }

        @Override
        public String getKey() {
            return key;
        }
    }

    public static class ResolvableDefaultKeyProperty<T> extends ResolvableKeyProperty<T> implements KeyDefaultProperty<T> {

        protected T defaultVal;

        public ResolvableDefaultKeyProperty(boolean cache, String key, T defaultVal, ConversionTolerantFunction<? extends T> fun) {
            super(cache, key, fun);
            this.defaultVal = defaultVal;
        }

        @Override
        public T resolveLogic(TolerantConfig... prop) {
            if (prop == null || prop.length == 0) {
                return getDefault();
            }
            for (int i = 0; i < prop.length; i++) {
                TolerantConfig conf = prop[i];
                if (conf == null) {
                    throw new IllegalArgumentException("TolerantConfig at index " + i + " is null");
                }
                if (conf.containsKey(key)) {
                    T resolved = fun.apply(conf);
                    if (resolved != null) {
                        return resolved;
                    }
                }

            }
            return getDefault();
        }

        @Override
        public T getDefault() {
            return defaultVal;
        }
    }

    public static class PreparedProp<T> extends ResolvableKeyProperty<T> {

        protected Supplier<List<TolerantConfig>> preparedConfigs;

        public PreparedProp(Supplier<List<TolerantConfig>> preparedConfigs, boolean cache, String key, ConversionTolerantFunction<? extends T> fun) {
            super(cache, key, fun);
            this.preparedConfigs = Objects.requireNonNull(preparedConfigs);
        }

        @Override
        public T resolveLogic(TolerantConfig... config) {
            return super.resolveLogic(
                    Stream.concat(
                            Stream.of(config),
                            preparedConfigs.get().stream()
                    ).toArray(s -> new TolerantConfig[s])
            );
        }
    }

    public static class PreparedDefaultProp<T> extends PreparedProp<T> implements KeyDefaultProperty<T> {

        protected T defaultVal;

        public PreparedDefaultProp(Supplier<List<TolerantConfig>> preparedConfigs, boolean cache, String key, T defaultVal, ConversionTolerantFunction<? extends T> fun) {
            super(preparedConfigs, cache, key, fun);
            this.defaultVal = defaultVal;
        }

        @Override
        public T resolveLogic(TolerantConfig... config) {
            T resolved = super.resolveLogic(config);
            if (resolved == null) {
                return getDefault();
            }
            return resolved;
        }

        @Override
        public T getDefault() {
            return defaultVal;
        }
    }

    public static class KP implements KeyProp.KeyVal<Object> {

        private final String key;
        private final Object val;

        public KP(String key, Object val) {
            this.key = key;
            this.val = val;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public Object getValue() {
            return val;
        }

    }

}
