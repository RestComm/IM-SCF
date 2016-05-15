/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011­2016, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */
package org.restcomm.imscf.common.util.logging;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.slf4j.MDC;
import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.MDCValueLevelPair;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * A TurboFilter for Logback. This class is similar to logback's {@link ch.qos.logback.classic.turbo.DynamicThresholdFilter},
 * but instead of exact matches, it is capable of partial matching (containment) or even regex matches.
 * It is also capable of checking a value against multiple MDC keys.
 * <p>
 * The default value for defaultThreshold is ERROR.<br/>
 * The default value for onHigherOrEqual is NEUTRAL.<br/>
 * The default value for onLower is DENY.<br/>
 */
public class MultiMatchingDynamicThresholdFilter extends TurboFilter {
    private List<Match> matches = new ArrayList<>();
    private Level defaultThreshold = Level.ERROR;
    private Set<String> keys = new HashSet<>();

    private FilterReply onHigherOrEqual = FilterReply.NEUTRAL;
    private FilterReply onLower = FilterReply.DENY;

    /**
     * How to match against MDC values.
     */
    public static enum MatchType {
        /** The MDC value must be exactly as specified in the &lt;Key>.*/
        EXACT,
        /** The MDC value must contain the &lt;Key>. This can be used for example for matching only the subscriber part of an MSISDN. */
        PARTIAL,
        /** The MDC value must match the regular expression specified in the &lt;Key>.*/
        REGEX
    }

    /**
     * An extension of {@link MDCValueLevelPair} with matchType. Default type is exact match.
     */
    public static class Match extends MDCValueLevelPair {
        private MatchType matchType = MatchType.EXACT;
        private Pattern p;

        public MatchType getMatchType() {
            return matchType;
        }

        public void setMatchType(MatchType matchType) {
            this.matchType = matchType;
        }

        public void compile() {
            this.p = Pattern.compile(getValue());
        }

        public boolean matches(String value) {
            return p.matcher(value).matches();
        }
    }

    /**
     * Add a match definition.
     */
    public void addMatch(Match match) {
        if (match.getValue() == null || match.getLevel() == null) {
            addError("Match value and level must be specified!");
            return;
        }
        if (match.getMatchType() == MatchType.REGEX) {
            try {
                match.compile();
            } catch (PatternSyntaxException e) {
                addError("Failed to parse regexp!", e);
                return;
            }
        }
        this.matches.add(match);
    }

    public void addKey(String key) {
        keys.add(key);
    }

    /**
     * Get the default threshold value when the MDC key is not set.
     *
     * @return the default threshold value in the absence of a set MDC key
     */
    public Level getDefaultThreshold() {
        return defaultThreshold;
    }

    public void setDefaultThreshold(Level defaultThreshold) {
        this.defaultThreshold = defaultThreshold;
    }

    /**
     * Get the FilterReply when the effective level is higher or equal to the
     * level of current logging request.
     *
     * @return FilterReply
     */
    public FilterReply getOnHigherOrEqual() {
        return onHigherOrEqual;
    }

    public void setOnHigherOrEqual(FilterReply onHigherOrEqual) {
        this.onHigherOrEqual = onHigherOrEqual;
    }

    /**
     * Get the FilterReply when the effective level is lower than the level of
     * current logging request.
     *
     * @return FilterReply
     */
    public FilterReply getOnLower() {
        return onLower;
    }

    public void setOnLower(FilterReply onLower) {
        this.onLower = onLower;
    }

    @Override
    public void start() {
        if (this.keys.isEmpty()) {
            addWarn("No key was specified");
        }
        if (this.matches.isEmpty()) {
            addWarn("No match was specified");
        }
        super.start();
    }

    /**
     * This method executes the following check for each value returned by the MDC keys.
     * It iterates over the match criteria to see if the MDC value matches any of the specified values using the specified MatchType.
     * If it does, the log level is checked against the specified Match's level to make a decision based on the values set in
     * {@link #defaultThreshold}, {@link #onHigherOrEqual} and {@link #onLower}.
     *
     * @return FilterReply - this filter's decision
     */
    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String s, Object[] objects, Throwable throwable) {
        if (!isStarted()) {
            return FilterReply.NEUTRAL;
        }

        Level levelAssociatedWithMDCValue = null;
        matchfound: for (String key : keys) {
            String mdcValue = MDC.get(key);
            if (mdcValue == null)
                continue; // check next key
            for (Match m : this.matches) {
                switch (m.getMatchType()) {
                case EXACT:
                    if (mdcValue.equals(m.getValue())) {
                        levelAssociatedWithMDCValue = m.getLevel();
                        break matchfound;
                    }
                    break;
                case PARTIAL:
                    if (mdcValue.contains(m.getValue())) {
                        levelAssociatedWithMDCValue = m.getLevel();
                        break matchfound;
                    }
                    break;
                case REGEX:
                    if (m.matches(mdcValue)) {
                        levelAssociatedWithMDCValue = m.getLevel();
                        break matchfound;
                    }
                    break;
                default:
                    break;
                }
            }
        }

        if (levelAssociatedWithMDCValue == null) { // no matches
            levelAssociatedWithMDCValue = defaultThreshold;
        }

        if (level.isGreaterOrEqual(levelAssociatedWithMDCValue)) {
            return onHigherOrEqual;
        } else {
            return onLower;
        }
    }
}
