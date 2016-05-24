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
package org.restcomm.imscf.common .diameter.creditcontrol;

import java.util.List;

/**
 * Class for storing diametergw call control response parameters.
 */
public class DiameterGWCreditControlResponse {

    protected CCAResultCode queryResult = null;
    protected String errorCode = null;
    protected String responseCaller = null;
    protected String responseCallee = null;
    protected String responseSmscAddress = null;

    public CCAResultCode getQueryResultObject() {
        return queryResult;
    }

    public void setQueryResult(CCAResultCode queryResult) {
        this.queryResult = queryResult;
    }

    public void setQueryResult(String queryResult) {
        this.queryResult = CCAResultCode.getCCRequestTypeByString(queryResult);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getResponseCaller() {
        return responseCaller;
    }

    public void setResponseCaller(String responseCaller) {
        this.responseCaller = responseCaller;
    }

    public String getResponseCallee() {
        return responseCallee;
    }

    public void setResponseCallee(String responseCallee) {
        this.responseCallee = responseCallee;
    }

    public String getResponseSmscAddress() {
        return responseSmscAddress;
    }

    public void setResponseSmscAddress(String responseSmscAddress) {
        this.responseSmscAddress = responseSmscAddress;
    }

    @Override
    public String toString() {
        String str = "DiameterGWCreditControlResponse {queryResult:"
                + ((queryResult == null) ? "null" : queryResult.toString()) + "," + "errorCode:"
                + ((errorCode == null) ? "null" : errorCode) + "," + "responseCaller:"
                + ((responseCaller == null) ? "null" : responseCaller) + "," + "responseCallee:"
                + ((responseCallee == null) ? "null" : responseCallee) + "," + "responseSmscAddress:"
                + ((responseSmscAddress == null) ? "null" : responseSmscAddress) + "}";
        return str;
    }

    public void loadFromWrapper(DiameterGWCCResponseJsonWrapper wrapper) {
        queryResult = null;
        errorCode = null;
        responseCaller = null;
        responseCallee = null;
        responseSmscAddress = null;

        List<DiameterGWCCParam> parameters = wrapper.getParams();

        for (DiameterGWCCParam p : parameters) {
            if (p.getName().equals("QueryResult")) {
                queryResult = CCAResultCode.getCCRequestTypeByString(p.getValue());
            } else if (p.getName().equals("ErrorCode")) {
                errorCode = p.getValue();
            } else if (p.getName().equals("ResponseCaller")) {
                responseCaller = p.getValue();
            } else if (p.getName().equals("ResponseCallee")) {
                responseCallee = p.getValue();
            } else if (p.getName().equals("ResponseSmscAddress")) {
                responseSmscAddress = p.getValue();
            }
        }

    }

}
