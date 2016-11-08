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
package org.restcomm.imscf.common .diameter.creditcontrol;

import java.util.List;

/**
 * Class for storing diametergw call control request parameters for diameter-http request.
 */
public class DiameterGWCCRequestJsonWrapper {
    private String caller;
    private String callee;
    private List<DiameterGWCCParam> params;

    public String getCallee() {
        return callee;
    }

    public void setCallee(String callee) {
        this.callee = callee;
    }

    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }

    public List<DiameterGWCCParam> getParams() {
        return params;
    }

    public void setParams(List<DiameterGWCCParam> params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return "Wrapper [ caller=" + caller + ", callee=" + callee + ", params=" + params + "]";
    }
}
