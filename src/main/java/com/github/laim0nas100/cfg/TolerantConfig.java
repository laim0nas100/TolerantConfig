package com.github.laim0nas100.cfg;

import com.github.laim0nas100.cfg.ConfigSettings.CachingConfSupplier;
import com.github.laim0nas100.cfg.ConfigSettings.ConfigurationSupplier;
import com.github.laim0nas100.cfg.KeyProp.KP;
import com.github.laim0nas100.cfg.KeyProp.KeyProperty;
import com.github.laim0nas100.cfg.KeyProp.KeyVal;
import com.github.laim0nas100.cfg.TolerantConfig.NestedInterpolation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * Error tolerant configuration based on key-value pairs. Generally of type
 * String:String, but can be used with any types ignoring most methods and just
 * using {@link TolerantConfig#getRaw(java.lang.String) }. Not intended to be
 * modified after loading, but you still can with exposed {@link TolerantConfig#getMap()
 * }, if the {@link ConfigurationSupplier} returns a static {@link Map}.
 *
 * @author laim0nas100
 */
public interface TolerantConfig {

    public static interface Interpolating extends TolerantConfig {

        /**
         * Override the getString call when interpolating
         *
         * @param key
         * @param defaultValue
         * @return
         */
        public default String getStringFromToken(String token, String defaultValue) {
            return getString(token, defaultValue);
        }

        /**
         * Actual interpolation implementation.
         *
         * @param value
         * @param defaultValue
         * @return
         */
        public String recursiveInterpolation(int limit, String value, String defaultValue);

        /**
         * Optional interpolate part of a string value. Assuming we are working
         * with string type all the way. Putting empty values if not found.
         *
         * @param value
         * @return
         */
        public default String iterpolateValue(String value) {
            return recursiveInterpolation(conf().recursiveInterpolationLimit(), value, "");
        }
    }

    public static class NestedInterpolation extends InterpolatingConfig {

        /**
         * Implementation details, when parsing nested interpolation
         */
        protected InterpolatingConfig nested;
        protected Set<String> insideTokens = new HashSet<>();
        protected final int limit;

        protected NestedInterpolation(InterpolatingConfig prev, int limit) {
            super(prev.supply);
            this.limit = limit;
            if (prev instanceof NestedInterpolation) {
                NestedInterpolation nest = (NestedInterpolation) prev;
                insideTokens.addAll(nest.insideTokens);
            }
        }

        @Override
        public String iterpolateValue(String value) {
            return recursiveInterpolation(limit, value, "");
        }

        @Override
        public String getStringFromToken(String token, String defaultValue) {
            if (limit <= 0 || insideTokens.contains(token)) {
                return defaultValue;
            }
            try {
                insideTokens.add(token);
                return super.getStringFromToken(token, defaultValue);
            } finally {
                insideTokens.remove(token);
            }

        }

    }

    public static class InterpolatingConfig extends DefaultTolerantConfig implements Interpolating {

        public InterpolatingConfig(ConfigurationSupplier supply) {
            super(supply);
        }

        protected InterpolatingConfig(InterpolatingConfig prev, int limit, String token) {
            super(prev.supply);
        }

        @Override
        public String recursiveInterpolation(int limit, String value, String defaultValue) {
            ConfigSettings settings = conf();
            if (!settings.interpolate() || limit <= 0) {
                return value;
            }
            String pre = settings.prefixInterpolation();
            int start = value.indexOf(pre);
            if (start < 0) {
                return value; // no iterpolation
            }
            String suff = settings.suffixInterpolation();
            int end = value.indexOf(suff, start);
            if (end <= start) {
                return value; // no iterpolation
            }

            String token = value.substring(start + pre.length(), end);
            String interpolated = null;
            if (settings.useEnv() && token.startsWith(settings.prefixEnv())) {
                //extract environment
                token = token.substring(settings.prefixEnv().length());
                String envProp = System.getenv(token);
                if (envProp == null) {
                    interpolated = defaultValue;
                } else {
                    if (settings.continueInterpolationEnvironment()) {
                        interpolated = new NestedInterpolation(this, limit - 1).getStringFromToken(envProp, defaultValue);
                    } else {
                        interpolated = envProp;
                    }
                }

            } else if (settings.useSys() && token.startsWith(settings.prefixSys())) {
                //extract system
                token = token.substring(settings.prefixSys().length());
                String property = System.getProperty(token);
                if (property == null) {
                    interpolated = defaultValue;
                } else {
                    if (settings.continueInterpolationEnvironment()) {
                        interpolated = new NestedInterpolation(this, limit - 1).getStringFromToken(property, defaultValue);
                    } else {
                        interpolated = property;
                    }
                }
            } else {
                // extract local
                interpolated = new NestedInterpolation(this, limit - 1).getStringFromToken(token, defaultValue);
            }

            return new StringBuilder()
                    .append(value.substring(0, start))
                    .append(interpolated)
                    // don't start new nesting fork, it's the same string just later on
                    .append(recursiveInterpolation(limit, value.substring(end + suff.length()), defaultValue)) 
                    .toString();

        }

        @Override
        public String str(Object key) {
            Object get = getMap().get(key);
            if (get == null) {
                return null;
            }
            return iterpolateValue(String.valueOf(get));
        }

    }

    public static class DefaultTolerantConfig implements TolerantConfig {

        protected final ConfigurationSupplier supply;

        public DefaultTolerantConfig(ConfigurationSupplier supply) {
            this.supply = Objects.requireNonNull(supply, "ConfigurationSupplier must not be null");
        }

        @Override
        public ConfigSettings conf() {
            return supply.getSettings();
        }

        @Override
        public Map getMap() {
            return supply.getConfOrNull();
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(getMap());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final DefaultTolerantConfig other = (DefaultTolerantConfig) obj;
            return Objects.equals(this.getMap(), other.getMap());
        }
    }

    public static final TolerantConfig empty = new DefaultTolerantConfig(
            ConfigurationSupplier.ofConfiguredMap(ConfigSettings.DEFAULT_CONFIG, Collections.EMPTY_MAP)
    );

    public static TolerantConfig empty() {
        return empty;
    }

    public static TolerantConfig of(Map conf) {
        if (conf == null) {
            return empty;
        }
        return new InterpolatingConfig(ConfigurationSupplier.ofConfiguredMap(ConfigSettings.DEFAULT_CONFIG, conf));
    }

    public static TolerantConfig of(ConfigSettings settings, Map conf) {
        if (conf == null) {
            return empty;
        }
        if (settings.interpolate()) {
            return new InterpolatingConfig(ConfigurationSupplier.ofConfiguredMap(settings, conf));
        } else {
            return new DefaultTolerantConfig(ConfigurationSupplier.ofConfiguredMap(settings, conf));
        }
    }

    /**
     * Combine multiple tolerant configs, caching the result, so combined map is
     * created only once. Using the first configuration settings.
     *
     * @param conf
     * @return
     */
    public static TolerantConfig combinedCached(TolerantConfig... conf) {
        if (conf.length == 0) {
            return empty;
        }
        Map[] maps = Stream.of(conf).map(m -> m.getMap()).toArray(s -> new Map[s]);
        return ofSuplierCached(ConfigurationSupplier.ofCombined(conf[0].conf(), maps));
    }

    /**
     * Combine multiple tolerant configs. Using the first configuration
     * settings.
     *
     * @param conf
     * @return
     */
    public static TolerantConfig combined(TolerantConfig... conf) {
        if (conf.length == 0) {
            return empty;
        }
        Util.CombinedMap combinedMap = new Util.CombinedMap(() -> {
            return Stream.of(conf).map(m -> m.getMap()).collect(Collectors.toList());
        });
        return ofSuplier(ConfigurationSupplier.ofConfiguredMap(conf[0].conf(), combinedMap));
    }

    public static TolerantConfig ofSuplier(final ConfigurationSupplier supply) {
        if (supply.getSettings().interpolate()) {
            return new InterpolatingConfig(supply);
        }
        return new DefaultTolerantConfig(supply);
    }

    public static TolerantConfig ofSuplierCached(final ConfigurationSupplier supply) {
        Objects.requireNonNull(supply, "Supplier must not be null");
        return ofSuplier(new CachingConfSupplier(supply));
    }

    public static interface ConversionTolerantFunction<T> extends Function<TolerantConfig, T> {

        public T convert(TolerantConfig t) throws Exception;

        @Override
        public default T apply(TolerantConfig t) {
            try {
                return convert(t);
            } catch (Exception ex) {
                return null;
            }
        }

    }

    public ConfigSettings conf();

    public Map getMap();

    /**
     * Return entries, ignoring interpolation.
     *
     * @return
     */
    public default Stream<? extends KeyVal> getEntriesRaw() {
        Map delegated = getMap();
        if (delegated == null) {
            return Stream.empty();
        }
        Stream<Map.Entry> stream = delegated.entrySet().stream();

        return stream.map(entry -> {
            return new KP(String.valueOf(entry.getKey()), entry.getValue());
        });
    }

    /**
     * Return entries, interpolating if configuration supports it.
     *
     * @return
     */
    public default Stream<? extends KeyVal> getEntries() {
        return getEntries(null);
    }

    /**
     * Return entries which starts with given prefix. Optionally interpolates.
     * Null prefix includes all entries.
     *
     * @param prefix
     * @return
     */
    public default Stream<? extends KeyVal> getEntries(String prefix) {
        Map map = getMap();
        if (map == null) {
            return Stream.empty();
        }
        final boolean interpolate = conf().interpolate();

        Stream<Map.Entry> stream = map.entrySet().stream();
        return stream.map(entry -> {
            Object key = entry.getKey();
            if (key == null) {
                return null;
            }
            String strKey = String.valueOf(key);
            if (prefix == null || strKey.startsWith(prefix)) {
                Object value = entry.getValue();
                if (interpolate && value instanceof String) {// possible interpolated
                    value = getString(strKey);
                }
                return new KP(strKey, value);
            }

            return null;
        }).filter(Objects::nonNull);

    }

    public default <T> T getByProp(KeyProperty<T> prop) {
        Objects.requireNonNull(prop, "KeyProperty must be supplied");
        return prop.resolve(this);
    }

    /**
     * Bypasses any default string decoration and interpolations. The only way
     * to receive a non-string value.
     *
     * @param <T>
     * @param key
     * @return
     */
    public default <T> T getRaw(String key) {
        return getOrThrow(key, f -> (T) f.getMap().get(key));
    }

    /**
     * Get the property converted to boolean or return the given default if
     * property was not present or conversion has failed
     *
     * @param key
     * @param or
     * @return
     */
    public default boolean getOr(String key, boolean or) {
        return getBoolean(key, or);
    }

    /**
     * Get the property converted to int or return the given default if property
     * was not present or conversion has failed
     *
     * @param key
     * @param or
     * @return
     */
    public default int getOr(String key, int or) {
        return getInt(key, or);
    }

    /**
     * Get the property converted to long or return the given default if
     * property was not present or conversion has failed
     *
     * @param key
     * @param or
     * @return
     */
    public default long getOr(String key, long or) {
        return getLong(key, or);
    }

    /**
     * Get the property converted to float or return the given default if
     * property was not present or conversion has failed
     *
     * @param key
     * @param or
     * @return
     */
    public default float getOr(String key, float or) {
        return getFloat(key, or);
    }

    /**
     * Get the property converted to double or return the given default if
     * property was not present or conversion has failed
     *
     * @param key
     * @param or
     * @return
     */
    public default double getOr(String key, double or) {
        return getDouble(key, or);
    }

    /**
     * Get the property converted to BigInteger or return the given default if
     * property was not present or conversion has failed
     *
     * @param key
     * @param or
     * @return
     */
    public default BigInteger getOr(String key, BigInteger or) {
        return getBigInteger(key, or);
    }

    /**
     * Get the property converted to BigDecimal or return the given default if
     * property was not present or conversion has failed
     *
     * @param key
     * @param or
     * @return
     */
    public default BigDecimal getOr(String key, BigDecimal or) {
        return getBigDecimal(key, or);
    }

    /**
     * Get the property as String or return the given default if property was
     * not present
     *
     * @param key
     * @param or
     * @return
     */
    public default String getOr(String key, String or) {
        return getString(key, or);
    }

    /**
     * Get the property converted to String array or return the given default if
     * property was not present or conversion has failed
     *
     * @param key
     * @param or
     * @return
     */
    public default String[] getOr(String key, String[] or) {
        return getStringArray(key, or);
    }

    /**
     * Get the property converted to specified type using the conversion
     * function or return the given default if property was not present or
     * conversion has failed
     *
     * @param func
     * @param ifNot
     * @return
     */
    public default <T> T getOr(ConversionTolerantFunction<T> func, T ifNot) {
        return Optional.ofNullable(this).map(func).orElse(ifNot);
    }

    /**
     * Get the property converted to specified type using the conversion
     * function or return the given default from a supplier if property was not
     * present or conversion has failed
     *
     * @param func
     * @param ifNot
     * @return
     */
    public default <T> T getOrSup(ConversionTolerantFunction<T> func, Supplier<? extends T> ifNot) {
        return Optional.ofNullable(this).map(func).orElseGet(ifNot);
    }

    /**
     * Get the property converted to specified type using the conversion
     * function or throw {@link NoSuchElementException} if property was not
     * present or conversion has failed
     *
     * @param key
     * @param func
     * @return
     */
    public default <T> T getOrThrow(String key, Function<TolerantConfig, T> func) {
        return Optional.ofNullable(this).map(func).orElseThrow(() -> new NoSuchElementException(key));
    }

    /**
     *
     * @return If backing map is empty.
     */
    public default boolean isEmpty() {
        return getMap().isEmpty();
    }

    /**
     *
     * @return size of the backing map.
     */
    public default int size() {
        return getMap().size();
    }

    /**
     * {@link TolerantConfig#str(java.lang.Object) } call followed by an
     * optional trim. Unprotected. Will throw if {@link TolerantConfig#getMap()
     * } throws.
     *
     */
    public default String strTrim(Object key) {
        String str = str(key);
        if (str == null) {
            return null;
        }
        if (conf().trimWhitespace()) {
            return str.trim();
        }
        return str;
    }

    /**
     * Main map call converting to string of returning null. Every method calls
     * this except {@link TolerantConfig#getRaw(java.lang.String) }.
     * Unprotected. Will throw if {@link TolerantConfig#getMap() } throws.
     *
     * @param key
     * @return
     */
    public default String str(Object key) {
        Object get = getMap().get(key);
        if (get == null) {
            return null;
        }
        return String.valueOf(get);
    }

    public default boolean containsKey(String key) {
        return getOr(p -> p.getMap().containsKey(key), false);
    }

    public default Object getProperty(String key) {
        return getOr(p -> p.getMap().get(key), null);
    }

    public default Iterator<String> getKeys(String prefix) {
        return getOr(p -> p.getMap()
                .keySet()
                .stream()
                .map(s -> {
                    if (s == null) {
                        return null;
                    }
                    String str = String.valueOf(s);
                    if (prefix == null || str.startsWith(prefix)) {
                        return str;
                    }
                    return null;
                })
                .filter(Objects::nonNull).iterator(), Collections.EMPTY_LIST.iterator()
        );
    }

    public default Iterator<String> getKeys() {
        return getKeys(null);
    }

    public default boolean getBoolean(String key) {
        return getOrThrow(key, p -> Boolean.valueOf(p.strTrim(key)));
    }

    public default Boolean getBoolean(String key, boolean defaultValue) {
        return getOr(p -> Boolean.valueOf(p.strTrim(key)), defaultValue);
    }

    public default Boolean getBoolean(String key, Boolean defaultValue) {
        return getOr(p -> Boolean.valueOf(p.strTrim(key)), defaultValue);
    }

    public default byte getByte(String key) {
        return getOrThrow(key, p -> Byte.valueOf(p.strTrim(key)));
    }

    public default Byte getByte(String key, byte defaultValue) {
        return getOr(p -> Byte.valueOf(p.strTrim(key)), defaultValue);
    }

    public default Byte getByte(String key, Byte defaultValue) {
        return getOr(p -> Byte.valueOf(p.strTrim(key)), defaultValue);
    }

    public default double getDouble(String key) {
        return getOrThrow(key, p -> Double.valueOf(p.strTrim(key)));
    }

    public default Double getDouble(String key, double defaultValue) {
        return getOr(p -> Double.valueOf(p.strTrim(key)), defaultValue);
    }

    public default Double getDouble(String key, Double defaultValue) {
        return getOr(p -> Double.valueOf(p.strTrim(key)), defaultValue);
    }

    public default float getFloat(String key) {
        return getOrThrow(key, p -> Float.valueOf(p.strTrim(key)));
    }

    public default float getFloat(String key, float defaultValue) {
        return getOr(p -> Float.valueOf(p.strTrim(key)), defaultValue);
    }

    public default float getFloat(String key, Float defaultValue) {
        return getOr(p -> Float.valueOf(p.strTrim(key)), defaultValue);
    }

    public default int getInt(String key) {
        return getOrThrow(key, p -> Integer.valueOf(p.strTrim(key)));
    }

    public default int getInt(String key, int defaultValue) {
        return getOr(p -> Integer.valueOf(p.strTrim(key)), defaultValue);
    }

    public default Integer getInt(String key, Integer defaultValue) {
        return getOr(p -> Integer.valueOf(p.strTrim(key)), defaultValue);
    }

    public default long getLong(String key) {
        return getOrThrow(key, p -> Long.valueOf(p.strTrim(key)));
    }

    public default long getLong(String key, long defaultValue) {
        return getOr(p -> Long.valueOf(p.strTrim(key)), defaultValue);
    }

    public default Long getLong(String key, Long defaultValue) {
        return getOr(p -> Long.valueOf(p.strTrim(key)), defaultValue);
    }

    public default short getShort(String key) {
        return getOrThrow(key, p -> Short.valueOf(p.strTrim(key)));
    }

    public default short getShort(String key, short defaultValue) {
        return getOr(p -> Short.valueOf(p.strTrim(key)), defaultValue);
    }

    public default Short getShort(String key, Short defaultValue) {
        return getOr(p -> Short.valueOf(p.strTrim(key)), defaultValue);
    }

    public default BigDecimal getBigDecimal(String key) {
        return getOrThrow(key, p -> new BigDecimal(p.strTrim(key)));
    }

    public default BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        return getOr(p -> new BigDecimal(p.strTrim(key)), defaultValue);
    }

    public default BigInteger getBigInteger(String key) {
        return getOrThrow(key, p -> new BigInteger(p.strTrim(key)));
    }

    public default BigInteger getBigInteger(String key, BigInteger defaultValue) {
        return getOr(p -> new BigInteger(p.strTrim(key)), defaultValue);
    }

    public default String getString(String key) {
        return getOrThrow(key, p -> p.str(key));
    }

    public default String getString(String key, String defaultValue) {
        return getOr(p -> p.str(key), defaultValue);
    }

    public default String[] getStringArray(String key) {
        return getOrThrow(key, p -> Util.splitArray(p.str(key), p.conf().listDelim(), p.conf().trimListWhiteSpace()));
    }

    public default String[] getStringArray(String key, String[] defaultValue) {
        return getOr(p -> Util.splitArray(p.str(key), p.conf().listDelim(), p.conf().trimListWhiteSpace()), defaultValue);
    }

    public default <C extends Enum<C>> C getEnum(String key, Class<C> enumType) {
        return getOrThrow(key, p -> {
            String name = p.strTrim(key);
            return Util.enumMatch(enumType, name)
                    .orElseThrow(() -> new IllegalArgumentException("Failed to match enum " + enumType.getSimpleName() + " by name " + name));

        });
    }

    public default <C extends Enum<C>> C getEnum(String key, C defaultEnum) {
        return getOr(p -> {
            return Util.enumMatch(defaultEnum.getDeclaringClass(), p.strTrim(key)).orElse(defaultEnum);
        }, defaultEnum);
    }

    public default List<String> getList(String key) {
        return getOrThrow(key, p -> Util.split(p.str(key), p.conf().listDelim(), p.conf().trimListWhiteSpace()));
    }

    public default List<String> getList(String key, List<String> defaultValue) {
        return getOr(p -> Util.split(p.str(key), p.conf().listDelim(), p.conf().trimWhitespace()), defaultValue);
    }

    /**
     * Returns a frozen truncated subset based on given prefix. Eager
     * interpolation.
     *
     * @param prefix
     * @return
     */
    public default TolerantConfig subset(String prefix) {
        return getOr(p -> TolerantConfig.of(p.conf().wihtoutIterpolation(), p.subMap(prefix)), TolerantConfig.empty);
    }

    /**
     * Get all current entries and put into a Properties object with full keys
     * which is then returned. Eager interpolation. Freezes the returned map.
     *
     * @param prefix
     * @return
     */
    public default Properties nonTruncatedPropertySubset(String prefix) {
        Properties prop = new Properties();
        prop.putAll(nonTruncatedSubMap(prefix));
        return prop;
    }

    /**
     * Get all current entries and put into a Properties object which is then
     * returned. Eager interpolation. Freezes the returned map.
     *
     * @return
     */
    public default Properties asProperties() {
        return asProperties(Function.identity(), Function.identity());
    }

    /**
     * Get all current entries and put into a Properties object with key and
     * object modification function which is then returned.
     *
     * Null keys are not inserted. Eager interpolation. Freezes the returned
     * map.
     *
     * @param keyMod
     * @param objectMod
     * @return
     */
    public default Properties asProperties(Function<String, String> keyMod, Function objectMod) {
        Objects.requireNonNull(keyMod, "Key modification function is empty");
        Objects.requireNonNull(objectMod, "Object modification function is null");
        Properties props = new Properties();
        getEntries().forEach(kv -> {
            String key = keyMod.apply(kv.getKey());
            if (key != null) {
                props.put(key, objectMod.apply(kv.getValue()));
            }
        });
        return props;
    }

    /**
     * Return a non-truncated sub Map based on given prefix. Freezes the
     * returned map.
     *
     * @param prefix
     * @return
     */
    public default Map nonTruncatedSubMap(String prefix) {
        LinkedHashMap subMap = new LinkedHashMap();
        getEntries(prefix).forEach(kv -> {
            String originalKey = kv.getKey();
            subMap.put(originalKey, kv.getValue());
        });
        return subMap;
    }

    /**
     * Return a truncated sub Map based on given prefix. Freezes the returned
     * map.
     *
     * @param prefix
     * @return
     */
    public default Map subMap(String prefix) {
        LinkedHashMap subMap = new LinkedHashMap();
        Stream<? extends KeyVal> entries = getEntries(prefix);
        String conf_nestingDelim = conf().nestingDelim();
        String fullPrefix = prefix + conf_nestingDelim;
        int fullLength = fullPrefix.length();
        entries.forEach(kv -> {
            String originalKey = kv.getKey();
            if (originalKey.startsWith(fullPrefix)) {
                String newKey = originalKey.substring(fullLength);
                subMap.put(newKey, kv.getValue());
            }
        });
        return subMap;
    }

}
