/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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
package org.restcomm.imscf.common.diameter.creditcontrol;

/**
 * Class for handling service endpoint access.
 */
public class ServiceEndpoint {

    private String url;
    private long bannedUntil;

    public ServiceEndpoint(String url) {
        setUrl(url);
    }

    public String getUrl() {
        return url;
    }

    private void setUrl(String url) {
        if (url == null) {
            throw new IllegalArgumentException("Service url cannot be null!");
        }
        this.url = url;
    }

    public boolean isBanned() {
        return bannedUntil > System.currentTimeMillis();
    }

    public void ban(int millisecs) {
        bannedUntil = System.currentTimeMillis() + millisecs;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ServiceEndpoint))
            return false;
        ServiceEndpoint other = (ServiceEndpoint) obj;
        return url.equals(other.url);
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }
}
