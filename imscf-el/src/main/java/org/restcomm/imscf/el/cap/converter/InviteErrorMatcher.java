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
package org.restcomm.imscf.el.cap.converter;

import org.restcomm.imscf.common.config.InviteErrorActionType;
import org.restcomm.imscf.common.config.InviteErrorHandlerType;
import org.restcomm.imscf.el.modules.routing.UnsignedNumberRangeList;

/** Class for matching an initial invite error response to a call handling action. */
public class InviteErrorMatcher {
    private InviteErrorActionType action;
    private UnsignedNumberRangeList statusCodes;
    private UnsignedNumberRangeList serviceKeys;

    public InviteErrorMatcher(InviteErrorHandlerType config) {
        this.action = config.getInviteErrorAction();
        // empty status code list is allowed, it will always match an error response (400-699)
        this.statusCodes = UnsignedNumberRangeList.parse(config.getInviteErrorRange());
        // empty service key list is allowed, it will match any service key
        this.serviceKeys = UnsignedNumberRangeList.parse(config.getServiceKeys());
    }

    public boolean matches(int responseStatus, int serviceKey) {
        // matches if all configured criteria match. A null (non-configured) criterion is a match.
        return (statusCodes == null || statusCodes.matches(responseStatus))
                && (serviceKeys == null || serviceKeys.matches(serviceKey));
    }

    public InviteErrorActionType getAction() {
        return action;
    }

    @Override
    public String toString() {
        return "[status " + statusCodes + ", sk " + serviceKeys + "]";
    }

}
