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
package org.restcomm.imscf.common.lwcomm.config.impl;

import org.restcomm.imscf.common.lwcomm.config.Node;
import org.restcomm.imscf.common.lwcomm.config.Route;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Default Route implementation.
 * @author Miklos Pocsaji
 *
 */
public final class RouteImpl extends Route {

    private String name;
    private Set<Node> possibleSources;
    private List<Node> destinations;
    private Mode mode;
    private List<Integer> retransmitPattern;
    private String defaultQueue;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Node> getPossibleSources() {
        return possibleSources;
    }

    public void setPossibleSources(Collection<Node> possibleSources) {
        this.possibleSources = new HashSet<Node>(possibleSources);
    }

    public List<Node> getDestinations() {
        return destinations;
    }

    public void setDestinations(List<Node> destinations) {
        this.destinations = destinations;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public List<Integer> getRetransmitPattern() {
        return retransmitPattern;
    }

    public void setRetransmitPattern(List<Integer> retransmitPattern) {
        this.retransmitPattern = retransmitPattern;
    }

    public String getDefaultQueue() {
        return defaultQueue;
    }

    public void setDefaultQueue(String defaultQueue) {
        this.defaultQueue = defaultQueue;
    }

}
