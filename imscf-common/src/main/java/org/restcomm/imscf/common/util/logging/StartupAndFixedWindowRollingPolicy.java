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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RolloverFailure;

/**
 * Extension of {@link FixedWindowRollingPolicy} to also immediately rotate on startup.
 */
// TODO: this could actually be tied to the application lifecycle somehow instead of relying on the fact that logging
// is initialized on startup, and this class is loaded at that point, initializing the set of files to be rotated.
public class StartupAndFixedWindowRollingPolicy extends FixedWindowRollingPolicy {

    // override max 20 in logback. Note: use with care...
    private static final int MAX_WINDOW_SIZE = 100;

    // static because we don't want to rotate again in case a configuration change causes a new instance to be created
    // instead we rotate once for each file this is used on.
    private static final Set<String> FILES_ROTATED_AT_STARTUP = Collections.synchronizedSet(new HashSet<String>());

    public StartupAndFixedWindowRollingPolicy() {
        // System.out.println("StartupAndFixedWindowRollingPolicy object created!");
    }

    @Override
    public void start() {
        super.start();
        String path = getActiveFileName();
        if (path != null && !FILES_ROTATED_AT_STARTUP.contains(path)) {
            FILES_ROTATED_AT_STARTUP.add(path);
            try {
                rollover();
                System.out.println("Executed initial rollover for file " + path);
            } catch (RolloverFailure e) {
                System.out.println("Failed to execute initial rollover for file " + path);
            }
        }
    }

    @Override
    protected int getMaxWindowSize() {
        return MAX_WINDOW_SIZE;
    }
}
