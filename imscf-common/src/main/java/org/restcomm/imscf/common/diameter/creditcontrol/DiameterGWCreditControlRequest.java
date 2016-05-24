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

import java.util.ArrayList;
import java.util.List;

/**
 * Class for storing diametergw call control request parameters.
 */
public class DiameterGWCreditControlRequest {
    protected String caller = null;
    protected String callee = null;
    protected CCRequestType requestType = null;
    protected String callerImsi = null;
    protected String vlrGt = null;
    protected String smscAddress = null;
    protected String smsSubmissionResult = null;

    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }

    public String getCallee() {
        return callee;
    }

    public void setCallee(String callee) {
        this.callee = callee;
    }

    public CCRequestType getRequestTypeObject() {
        return requestType;
    }

    public String getRequestType() {
        return requestType.toString();
    }

    public void setRequestType(CCRequestType requestType) {
        this.requestType = requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = CCRequestType.getCCRequestTypeByString(requestType);
    }

    public String getCallerImsi() {
        return callerImsi;
    }

    public void setCallerImsi(String callerImsi) {
        this.callerImsi = callerImsi;
    }

    public String getVlrGt() {
        return vlrGt;
    }

    public void setVlrGt(String vlrGt) {
        this.vlrGt = vlrGt;
    }

    public String getSmscAddress() {
        return smscAddress;
    }

    public void setSmscAddress(String smscAddress) {
        this.smscAddress = smscAddress;
    }

    public String getSmsSubmissionResult() {
        return smsSubmissionResult;
    }

    public void setSmsSubmissionResult(String smsSubmissionResult) {
        this.smsSubmissionResult = smsSubmissionResult;
    }

    @Override
    public String toString() {
        String str = "DiameterGWCreditControlRequest {caller:" + ((caller == null) ? "null" : caller) + "," + "callee:"
                + ((callee == null) ? "null" : callee) + "," + "requestType:"
                + ((requestType == null) ? "null" : requestType.toString()) + "," + "callerImsi:"
                + ((callerImsi == null) ? "null" : callerImsi) + "," + "vlrGt:" + ((vlrGt == null) ? "null" : vlrGt)
                + "," + "smscAddress:" + ((smscAddress == null) ? "null" : smscAddress) + "," + "smsSubmissionResult:"
                + ((smsSubmissionResult == null) ? "null" : smsSubmissionResult) + "}";
        return str;
    }

    public DiameterGWCCRequestJsonWrapper getWrapper() {
        DiameterGWCCRequestJsonWrapper wrapper = new DiameterGWCCRequestJsonWrapper();
        List<DiameterGWCCParam> parameters = new ArrayList<DiameterGWCCParam>();

        wrapper.setCaller(caller);
        wrapper.setCallee(callee);

        parameters.add(new DiameterGWCCParam("RequestType", getRequestType()));

        if (getRequestTypeObject() == CCRequestType.BALANCE) {
            parameters.add(new DiameterGWCCParam("CallerImsi", getCallerImsi()));
            parameters.add(new DiameterGWCCParam("VlrGt", getVlrGt()));
            parameters.add(new DiameterGWCCParam("SmscAddress", getSmscAddress()));
        }

        if (getRequestTypeObject() == CCRequestType.DEBIT) {
            parameters.add(new DiameterGWCCParam("SmsSubmissionResult", getSmsSubmissionResult()));
        }

        wrapper.setParams(parameters);
        return wrapper;
    }
}
