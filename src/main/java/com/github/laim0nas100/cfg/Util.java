package com.github.laim0nas100.cfg;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

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

        throw new IllegalArgumentException("Failed to map " + type + " enum not found for name:" + name);
    }
}
