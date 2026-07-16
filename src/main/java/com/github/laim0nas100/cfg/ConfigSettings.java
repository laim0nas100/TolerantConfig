package com.github.laim0nas100.cfg;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 *
 * Config settings.
 *
 * @author laim0nas100
 */
public interface ConfigSettings {

    /**
     * Convenient class to simply change variables for desired configuration
     */
    public static class MutableConfigSettings implements ConfigSettings {

        public String tokenStart = INTERP_TOKEN_START;
        public String tokenEnd = INTERP_TOKEN_END;
        public String listDelim = LIST_DELIM;
        public String nestingDelim = NESTING_DELIM;
        public String systemPrefix = SYSTEM_PREFIX;
        public String envPrefix = ENVIRONMENT_PREFIX;
        public boolean useInterpolation = true;
        public boolean useEnvironmentInterpolation = useInterpolation;
        public boolean useSystemInterpolation = useInterpolation;
        public boolean continueInterpolationSystem = false;
        public boolean continueInterpolationEnvironment = false;
        public boolean trimWhitespace = true;
        public boolean trimListWhitespace = false;
        public int recursiveInterpolationLimit = INTERPOLATION_LIMIT;

        @Override
        public String listDelim() {
            return listDelim;
        }

        @Override
        public String nestingDelim() {
            return nestingDelim;
        }

        @Override
        public boolean trimWhitespace() {
            return trimWhitespace;
        }

        @Override
        public boolean trimListWhiteSpace() {
            return trimListWhitespace;
        }

        @Override
        public boolean interpolate() {
            return useInterpolation;
        }

        @Override
        public boolean useSys() {
            return useSystemInterpolation;
        }

        @Override
        public boolean useEnv() {
            return useEnvironmentInterpolation;
        }

        @Override
        public String prefixInterpolation() {
            return tokenStart;
        }

        @Override
        public String suffixInterpolation() {
            return tokenEnd;
        }

        @Override
        public String prefixSys() {
            return systemPrefix;
        }

        @Override
        public String prefixEnv() {
            return envPrefix;
        }

        @Override
        public int recursiveInterpolationLimit() {
            return recursiveInterpolationLimit;
        }

        @Override
        public boolean continueInterpolationSystem() {
            return continueInterpolationSystem;
        }

        @Override
        public boolean continueInterpolationEnvironment() {
            return continueInterpolationEnvironment;
        }

    }

    /**
     * Default config settings.
     */
    public static final ConfigSettings DEFAULT_CONFIG = new ConfigSettings() {
    };

    /**
     * Default config settings without interpolation.
     */
    public static final ConfigSettings DEFAULT_NO_ITERP = new ConfigSettings() {
        @Override
        public boolean useEnv() {
            return false;
        }

        @Override
        public boolean useSys() {
            return false;
        }

        @Override
        public boolean interpolate() {
            return false;
        }

    };

    public static final String INTERP_TOKEN_START = "${";
    public static final String INTERP_TOKEN_END = "}";

    public static final String LIST_DELIM = ";";
    public static final String NESTING_DELIM = ".";

    public static final String SYSTEM_PREFIX = "sys:";
    public static final String ENVIRONMENT_PREFIX = "env:";

    public static final Integer INTERPOLATION_LIMIT = 128; // arbitrary limit, lees than jvm stack siZe.

    /**
     * Delimiter to separate values inside a list or array
     */
    public default String listDelim() {
        return LIST_DELIM;
    }

    /**
     * Delimiter to separate nested values
     */
    public default String nestingDelim() {
        return NESTING_DELIM;
    }

    /**
     * Toggle trimming of strings when parsing. Only applies to singular values
     * in non-strings. Default true.
     */
    public default boolean trimWhitespace() {
        return true;
    }

    /**
     * Toggle trimming of string arrays and lists when parsing. Default false.
     *
     */
    public default boolean trimListWhiteSpace() {
        return false;
    }

    /**
     * Toggle basic interpolation of the same set. Required to be true to use
     * System and Environment interpolation, (master switch). Default true.
     *
     */
    public default boolean interpolate() {
        return true;
    }

    /**
     * Toggle to look up system variables.
     * {@link System#getProperty(java.lang.String) }
     * Default true.
     *
     */
    public default boolean useSys() {
        return true;
    }

    /**
     * Toggle to look up environment variables.
     * {@link System#getEnv(java.lang.String) }
     * Default true.
     *
     */
    public default boolean useEnv() {
        return true;
    }

    /**
     *
     * @return Interpolation token prefix.
     */
    public default String prefixInterpolation() {
        return INTERP_TOKEN_START;
    }

    /**
     *
     * @return Interpolation token suffix.
     */
    public default String suffixInterpolation() {
        return INTERP_TOKEN_END;
    }

    /**
     *
     * @return {@link System#getenv(java.lang.String) } look up interpolation
     * key prefix.
     */
    public default String prefixSys() {
        return SYSTEM_PREFIX;
    }

    /**
     *
     * @return {@link System#getProperty(java.lang.String) } look up
     * interpolation key prefix.
     */
    public default String prefixEnv() {
        return ENVIRONMENT_PREFIX;
    }

    /**
     *
     * @return Interpolation recursion limit. Should be sane. Limited only by
     * jvm stack size. Can interpolate self, but will resolve to an empty
     * string.
     */
    public default int recursiveInterpolationLimit() {
        return INTERPOLATION_LIMIT;
    }

    /**
     * Rare case when you want to interpolate a system variable. Default false.
     */
    public default boolean continueInterpolationSystem() {
        return false;
    }

    /**
     * Rare case when you want to interpolate an environment variable. Default
     * false.
     */
    public default boolean continueInterpolationEnvironment() {
        return false;
    }

    /**
     * Turns off interpolation while preserving every other setting.
     * @return 
     */
    public default ConfigSettings wihtoutIterpolation() {
        ConfigSettings me = this;
        return new ConfigSettings() {
            @Override
            public String listDelim() {
                return me.listDelim();
            }

            @Override
            public String nestingDelim() {
                return me.nestingDelim();
            }

            @Override
            public boolean trimWhitespace() {
                return me.trimWhitespace();
            }

            @Override
            public boolean trimListWhiteSpace() {
                return me.trimListWhiteSpace();
            }

            @Override
            public boolean interpolate() {
                return false;
            }

            @Override
            public boolean useSys() {
                return me.useSys();
            }

            @Override
            public boolean useEnv() {
                return me.useEnv();
            }

            @Override
            public String prefixInterpolation() {
                return me.prefixInterpolation();
            }

            @Override
            public String suffixInterpolation() {
                return me.suffixInterpolation();
            }

            @Override
            public String prefixSys() {
                return me.prefixSys();
            }

            @Override
            public String prefixEnv() {
                return me.prefixEnv();
            }

            @Override
            public int recursiveInterpolationLimit() {
                return me.recursiveInterpolationLimit();
            }

            @Override
            public boolean continueInterpolationSystem() {
                return me.continueInterpolationSystem();
            }

            @Override
            public boolean continueInterpolationEnvironment() {
                return me.continueInterpolationEnvironment();
            }

            @Override
            public ConfigSettings wihtoutIterpolation() {
                return me.wihtoutIterpolation();
            }
        };
    }

    public static interface ConfigurationSupplier {

        public static ConfigurationSupplier ofCombined(ConfigSettings settings, Map... maps) {
            Objects.requireNonNull(settings);
            Objects.requireNonNull(maps);
            return new ConfigurationSupplier() {
                @Override
                public Map getConfiguration() throws IOException {
                    if (maps.length == 0) {
                        return Collections.EMPTY_MAP;
                    }
                    LinkedHashMap combinedMap = new LinkedHashMap();
                    for (int i = maps.length - 1; i >= 0; i--) {
                        combinedMap.putAll(maps[i]);
                    }
                    return combinedMap;
                }

                @Override
                public ConfigSettings getSettings() {
                    return settings;
                }
            };
        }

        public static ConfigurationSupplier ofConfiguredMap(ConfigSettings settings, Map map) {
            Objects.requireNonNull(settings);
            Objects.requireNonNull(map);
            return new ConfigurationSupplier() {
                @Override
                public Map getConfiguration() throws IOException {
                    return map;
                }

                @Override
                public ConfigSettings getSettings() {
                    return settings;
                }
            };
        }

        public default ConfigSettings getSettings() {
            return ConfigSettings.DEFAULT_CONFIG;
        }

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

        protected final boolean sync;
        protected final ConfigurationSupplier supply;
        protected Map conf;
        protected ConfigSettings settings;

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
        public ConfigSettings getSettings() {
            if (settings != null && !supply.isChanged()) {
                return settings;
            }
            if (sync) {
                synchronized (supply) {
                    settings = supply.getSettings();
                }
            } else {
                settings = supply.getSettings();
            }
            return settings;
        }

        @Override
        public boolean isChanged() {
            return supply.isChanged();
        }
    }

}
