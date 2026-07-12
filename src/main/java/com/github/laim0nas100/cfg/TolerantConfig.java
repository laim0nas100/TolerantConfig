package com.github.laim0nas100.cfg;

import com.github.laim0nas100.cfg.KeyProp.KP;
import com.github.laim0nas100.cfg.KeyProp.KeyProperty;
import com.github.laim0nas100.cfg.KeyProp.KeyVal;
import static com.github.laim0nas100.cfg.KeyProp.LIST_DELIM;
import static com.github.laim0nas100.cfg.KeyProp.NESTING_DELIM;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
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

/**
 *
 * Error tolerant configuration based on key-value pairs. Generally of type
 * String:String, but can be used with any types ignoring most methods and just
 * using {@link TolerantConfig#getAny(java.lang.String) }. Not intended to be
 * modified after loading, but you still can with exposed {@link TolerantConfig#getMap()
 * }, if the {@link ConfigurationSupplier} returns a static {@link Map}.
 *
 * @author laim0nas100
 */
public interface TolerantConfig {

    public default String conf_listDelim() {
        return LIST_DELIM;
    }

    public default String conf_nestingDelim() {
        return NESTING_DELIM;
    }

    /**
     * Toggle trimming of strings when parsing. Only applies to singular values
     * in non-strings. Default true.
     */
    public default boolean conf_trimWhitespace() {
        return true;
    }

    /**
     * Toggle trimming of string arrays and lists when parsing. Default false.
     *
     * @return
     */
    public default boolean conf_trimListWhiteSpace() {
        return false;
    }

    public static class DefaultTolerantConfig implements TolerantConfig {

        protected final ConfigurationSupplier supply;

        public DefaultTolerantConfig(ConfigurationSupplier supply) {

            this.supply = Objects.requireNonNull(supply, "ConfigurationSupplier must not be null");
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

    public static final TolerantConfig empty = new DefaultTolerantConfig(() -> Collections.EMPTY_MAP);

    public static TolerantConfig empty() {
        return empty;
    }

    public static TolerantConfig of(Map conf) {
        if (conf == null) {
            return empty;
        }
        return new DefaultTolerantConfig(() -> conf);
    }

    /**
     * Combine multiple tolerant configs using the {@link OverrideCombiner}
     * policy.
     *
     * @param conf
     * @return
     */
    public static TolerantConfig combined(TolerantConfig... conf) {
        if (conf.length == 0) {
            return empty;
        }
        LinkedHashMap conbinedMap = new LinkedHashMap();
        for (int i = conf.length - 1; i >= 0; i--) {
            Map delegated = conf[i].getMap();
            conbinedMap.putAll(delegated);
        }
        return of(conbinedMap);
    }

    public static TolerantConfig ofSuplier(final ConfigurationSupplier supply) {
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

    public static interface ConfigurationSupplier {

        public Map getConfiguration() throws IOException;

        public default Map getConfOrNull() {
            try {
                return getConfiguration();
            } catch (IOException ex) {
                return null;
            }
        }

        public default boolean isChanged() { // might want to hot reload
            return false;
        }

    }

    public static class CachingConfSupplier implements ConfigurationSupplier {

        protected final ConfigurationSupplier supply;
        protected Map conf;
        protected final boolean sync;

        public CachingConfSupplier(ConfigurationSupplier real) {
            this(real, true);
        }

        public CachingConfSupplier(ConfigurationSupplier real, boolean sync) {
            this.supply = Objects.requireNonNull(real);
            this.sync = sync;
        }

        @Override
        public Map getConfiguration() throws IOException {
            if (conf != null && !supply.isChanged()) {
                return conf;
            }
            if (sync) {
                synchronized (supply) {
                    conf = supply.getConfOrNull();
                }
            } else {
                conf = supply.getConfOrNull();
            }

            return conf;
        }

        @Override
        public boolean isChanged() {
            return supply.isChanged();
        }

    }

    public Map getMap();

    public default Iterator<? extends KeyVal> getEntries() {
        Map delegated = getMap();
        if (delegated == null) {
            return Collections.emptyIterator();
        }
        Iterator<Map.Entry> iterator = delegated.entrySet().iterator();
        return new Iterator<KeyVal>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public KeyVal next() {
                Map.Entry next = iterator.next();
                return new KP(String.valueOf(next.getKey()), next.getValue());
            }
        };
    }

    public default Iterator<? extends KeyVal> getEntries(String prefix) {
        Map map = getMap();
        if (map == null) {
            return Collections.emptyIterator();
        }
        Set<Map.Entry> entrySet = map.entrySet();

        return entrySet.stream()
                .map(entry -> {
                    Object key = entry.getKey();
                    if (key == null) {
                        return null;
                    }
                    String strKey = String.valueOf(key);
                    if (strKey.startsWith(prefix)) {
                        return new KP(strKey, entry.getValue());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .iterator();
    }

    public default <T> T getByProp(KeyProperty<T> prop) {
        Objects.requireNonNull(prop, "KeyProperty must be supplied");
        return prop.resolve(this);
    }

    public default <T> T getAny(String key) {
        return getOrThrow(key, f -> (T) f.getMap().get(key));
    }

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

    public default boolean isEmpty() {
        return getMap().isEmpty();
    }

    public default int size() {
        return getMap().size();
    }

    public default String strTrim(Object key) {
        String str = str(key);
        if (str == null) {
            return null;
        }
        if (conf_trimWhitespace()) {
            return str.trim();
        }
        return str;
    }

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
        return getOr(p -> p.getMap().<Set<Object>>keySet().stream().filter(Objects::nonNull).map(s -> String.valueOf(s)).iterator(), Collections.EMPTY_LIST.iterator());
    }

    public default Iterator<String> getKeys() {
        return getOr(p -> p.getMap().keySet().stream().map(s -> String.valueOf(s)).iterator(), Collections.EMPTY_LIST.iterator());
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
        return getOrThrow(key, p -> Util.splitArray(p.str(key), p.conf_listDelim(), p.conf_trimListWhiteSpace()));
    }

    public default String[] getStringArray(String key, String[] defaultValue) {
        return getOr(p -> Util.splitArray(p.str(key), p.conf_listDelim(), p.conf_trimListWhiteSpace()), defaultValue);
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
        return getOrThrow(key, p -> Util.split(p.str(key), p.conf_listDelim(), p.conf_trimListWhiteSpace()));
    }

    public default List<String> getList(String key, List<String> defaultValue) {
        return getOr(p -> Util.split(p.str(key), p.conf_listDelim(), p.conf_trimWhitespace()), defaultValue);
    }

    public default TolerantConfig immutableSubset(String prefix) {
        return getOr(p -> TolerantConfig.of(p.subMap(prefix)), TolerantConfig.empty);
    }

    /**
     * Get all current entries and put into a Properties object with full keys
     * which is then returned.
     *
     * @param prefix
     * @return
     */
    public default Properties nonTruncatedPropertySubset(String prefix) {
        Map nonTruncatedSubMap = nonTruncatedSubMap(prefix);
        Properties prop = new Properties(nonTruncatedSubMap.size());
        prop.putAll(nonTruncatedSubMap);
        return prop;
    }

    /**
     * Get all current entries and put into a Properties object which is then
     * returned.
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
     * Null keys are not inserted
     *
     * @param keyMod
     * @param objectMod
     * @return
     */
    public default Properties asProperties(Function<String, String> keyMod, Function objectMod) {
        Objects.requireNonNull(keyMod, "Key modification function is empty");
        Objects.requireNonNull(objectMod, "Object modification function is null");
        Properties props = new Properties();
        Iterator<? extends KeyVal> entries = getEntries();
        while (entries.hasNext()) {
            KeyVal kv = entries.next();
            String key = keyMod.apply(kv.getKey());
            if (key == null) {
                continue;
            }
            props.put(key, objectMod.apply(kv.getValue()));
        }
        return props;
    }

    public default Map nonTruncatedSubMap(String prefix) {
        LinkedHashMap subMap = new LinkedHashMap();
        Iterator<? extends KeyVal> entries = getEntries(prefix);
        while (entries.hasNext()) {
            KeyVal next = entries.next();
            String originalKey = next.getKey();
            subMap.put(originalKey, next.getValue());
        }
        return subMap;
    }

    public default Map subMap(String prefix) {
        LinkedHashMap subMap = new LinkedHashMap();
        Iterator<? extends KeyVal> entries = getEntries(prefix);
        String conf_nestingDelim = conf_nestingDelim();
        String fullPrefix = prefix + conf_nestingDelim;
        int fullLength = fullPrefix.length();
        while (entries.hasNext()) {
            KeyVal next = entries.next();
            String originalKey = next.getKey();
            if (originalKey.startsWith(fullPrefix)) {
                String newKey = originalKey.substring(fullLength);
                subMap.put(newKey, next.getValue());
            }
        }
        return subMap;
    }

}
