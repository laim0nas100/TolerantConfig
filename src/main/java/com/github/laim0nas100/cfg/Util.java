package com.github.laim0nas100.cfg;

import com.github.laim0nas100.cfg.Util.BiOperatorOpt.Terminating;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 *
 * @author laim0nas100
 */
public abstract class Util {

    /**
     * Splits the string by occurrences of the given literal separator string.
     * <p>
     * This method behaves like {@link String#split(String)} with a literal
     * separator, but preserves trailing empty strings and does not interpret
     * the separator as a regex.
     * <p>
     * <b>Examples:</b>
     * <pre>{@code
     * split("a,b,c", ",")     → ["a", "b", "c"]
     * split("a,,b,", ",")     → ["a", "", "b", ""]
     * split("", ",")          → [""]
     * split("hello", "xyz")   → ["hello"]
     * split("a;b;c", ";")     → ["a", "b", "c"]
     * }</pre>
     *
     * @param source the input string (must not be null)
     * @param separator the literal delimiter (must not be null or empty)
     * @param trimWhiteSpace to trim the trailing/leading whitespace
     * @return array of substrings (never null, may contain empty strings)
     * @throws NullPointerException if source or separator is null
     * @throws IllegalArgumentException if separator is empty
     */
    public static ArrayList<String> split(String source, String separator, boolean trimWhiteSpace) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(separator, "separator");
        if (separator.isEmpty()) {
            throw new IllegalArgumentException("separator must not be empty");
        }
        ArrayList<String> parts = new ArrayList<>();
        int index = 0;
        int len = separator.length();
        if (len == 1) {//split by char
            char sep = separator.charAt(0);
            for (;;) {
                int next = source.indexOf(sep, index);
                if (next < 0) {
                    String part = source.substring(index);
                    if (trimWhiteSpace) {
                        part = part.trim();
                    }
                    parts.add(part);
                    break;
                }
                String part = source.substring(index, next);
                if (trimWhiteSpace) {
                    part = part.trim();
                }
                parts.add(part);
                index = next + 1;
            }
        } else {
            for (;;) {
                int next = source.indexOf(separator, index);
                if (next < 0) {
                    String part = source.substring(index);
                    if (trimWhiteSpace) {
                        part = part.trim();
                    }
                    parts.add(part);
                    break;
                } else {
                    String part = source.substring(index, next);
                    if (trimWhiteSpace) {
                        part = part.trim();
                    }
                    parts.add(part);
                    index = next + len;
                }
            }
        }
        return parts;
    }

    /**
     * Delegates to {@link Util#split(java.lang.String, java.lang.String) {
     *
     * @param source
     * @param separator
     * @return
     */
    public static String[] splitArray(String source, String separator, boolean trimWhiteSpace) {
        return split(source, separator, trimWhiteSpace).stream().toArray(s -> new String[s]);
    }

    /**
     * Checks if a CharSequence is empty (""), null or whitespace only.
     *
     * <p>
     * Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
     *
     * <pre>
     * StringUtils.isBlank(null)      = true
     * StringUtils.isBlank("")        = true
     * StringUtils.isBlank(" ")       = true
     * StringUtils.isBlank("bob")     = false
     * StringUtils.isBlank("  bob  ") = false
     * </pre>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is null, empty or whitespace
     * only
     */
    public static boolean isBlank(final CharSequence cs) {
        final int strLen = cs == null ? 0 : cs.length();
        if (strLen == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tries to match enum name, ignoring case
     *
     * @param <C> enum
     * @param type class of enum
     * @param name name to match
     * @return
     */
    public static <C extends Enum<C>> Optional<C> enumMatch(Class<C> type, String name) {
        if (!isBlank(name)) {
            EnumSet<C> allOf = EnumSet.allOf(type);
            for (C e : allOf) {
                if (e.name().equalsIgnoreCase(name)) {
                    return Optional.of(e);
                }
            }
        }
        return Optional.empty();
    }

    public static interface BiOperatorOpt<T> {

        public static class Terminating<T> {

            public final boolean terminate;
            public final T value;

            public Terminating(boolean terminate, T value) {
                this.terminate = terminate;
                this.value = value;
            }

            public static <A> Terminating<A> val(A value) {
                return new Terminating<>(false, value);
            }

            public static <A> Terminating<A> end(A value) {
                return new Terminating<>(true, value);
            }

        }

        public Terminating<T> terminatingApply(T t, T u);

    }

    public static class CombinedMap<K, V> implements Map<K, V> {

        public CombinedMap(Supplier<List<Map<K, V>>> mapSupplier) {
            this.mapSupplier = mapSupplier;
        }

        protected Supplier<List<Map<K, V>>> mapSupplier;

        protected <U> U reduce(U identity,
                BiFunction<U, ? super Map<K, V>, U> accumulator,
                BiOperatorOpt<U> combiner) {
            U result = identity;
            List<Map<K, V>> get = mapSupplier.get();
            for (Map<K, V> map : get) {
                BiOperatorOpt.Terminating<U> terminatingApply = combiner.terminatingApply(result, accumulator.apply(result, map));
                if (terminatingApply.terminate) {
                    return terminatingApply.value;
                }
                result = terminatingApply.value;
            }
            return result;
        }

        @Override
        public int size() {
            return reduce(0, (s, m) -> m.size(), (sum, s) -> Terminating.val(sum + s));
        }

        @Override
        public boolean isEmpty() {
            return reduce(true, (s, m) -> m.isEmpty(), (foundFilled, mapEmpty) -> {
                if (!mapEmpty) {
                    return Terminating.end(false);
                }
                return Terminating.val(true);
            });
        }

        @Override
        public boolean containsKey(Object key) {
            return reduce(false, (s, m) -> m.containsKey(key), (foundKey, mapContains) -> {
                if (mapContains) {
                    return Terminating.end(true); // terminating
                }
                return Terminating.val(false);
            });
        }

        @Override
        public boolean containsValue(Object value) {
            return reduce(false, (s, m) -> m.containsValue(value), (foundKey, mapContains) -> {
                if (mapContains) {
                    return Terminating.end(true); // terminating
                }
                return Terminating.val(false);
            });
        }

        @Override
        public V get(Object key) {
            return reduce(null, (s, m) -> m.get(key), (foundResult, getResult) -> {
                if (getResult != null) {
                    return Terminating.end(getResult); // terminating
                }
                return Terminating.val(null);
            });
        }

        protected RuntimeException assertWrite() {
            return new UnsupportedOperationException("Read only, combined map");
        }

        @Override
        public V put(K key, V value) {
            throw assertWrite();
        }

        @Override
        public V remove(Object key) {
            throw assertWrite();
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            throw assertWrite();
        }

        @Override
        public void clear() {
            throw assertWrite();
        }

        @Override
        public Set<K> keySet() {
            Set<K> keys = new LinkedHashSet<>();
            return reduce(keys, (s, m) -> m.keySet(), (totalSet, keySet) -> {
                totalSet.addAll(keySet);
                return Terminating.val(totalSet);
            });
        }

        @Override
        public Collection<V> values() {
            Collection<V> vals = new ArrayList<>();
            return reduce(vals, (s, m) -> m.values(), (total, values) -> {
                total.addAll(values);
                return Terminating.val(total);
            });
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            Set<Entry<K, V>> entrySet = new LinkedHashSet<>();
            return reduce(entrySet, (s, m) -> m.entrySet(), (total, entries) -> {
                total.addAll(entries);
                return Terminating.val(total);
            });
        }

    }
}
