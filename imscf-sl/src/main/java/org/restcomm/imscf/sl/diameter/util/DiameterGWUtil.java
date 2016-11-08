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
package org.restcomm.imscf.sl.diameter.util;

import org.restcomm.imscf.sl.diameter.DiameterGW;
import org.restcomm.imscf.sl.diameter.creditcontrol.DiameterGWCCASessionData;
import org.restcomm.imscf.sl.diameter.creditcontrol.SubscriptionIdType;
import org.restcomm.imscf.common.diameter.creditcontrol.CCAResultCode;
import org.restcomm.imscf.common.diameter.creditcontrol.CCRequestType;
import org.restcomm.imscf.common.diameter.creditcontrol.DiameterGWCreditControlRequest;
import org.restcomm.imscf.common.diameter.creditcontrol.DiameterGWCreditControlResponse;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;

import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.cca.ServerCCASession;
import org.jdiameter.api.cca.events.JCreditControlRequest;
import org.jdiameter.client.api.IAnswer;
import org.jdiameter.client.impl.parser.ElementParser;
import org.jdiameter.client.impl.parser.MessageImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for processing diametergw requests and responses.
 */
public final class DiameterGWUtil {

    private static Logger logger = LoggerFactory.getLogger(DiameterGWUtil.class);

    public static final int SDP_SERVICE_PARAMETER_INFO = 999;
    public static final Long RESTCOMM_VENDOR_ID = 42215L;

    private DiameterGWUtil() {
    }

    public static DiameterGWCreditControlRequest createCreditControlRequestForINService(
            JCreditControlRequest diameterRequest) {
        logger.debug("Diameter credit control request creation for IN service.");
        DiameterGWCreditControlRequest diameterGWCreditControlRequest = null;

        try {
            Long ccRequestTypeCode = diameterRequest.getMessage().getAvps().getAvp(Avp.CC_REQUEST_TYPE).getUnsigned32();

            // <type type-name="Enumerated">
            // <enum code="1" name="INITIAL_REQUEST" />
            // <enum code="2" name="UPDATE_REQUEST" />
            // <enum code="3" name="TERMINATION_REQUEST" />
            // <enum code="4" name="EVENT_REQUEST" />
            // </type>

            CCRequestType ccRequestType = CCRequestType.getCCRequestTypeByCode(ccRequestTypeCode);
            Long ccRequestNumber = diameterRequest.getMessage().getAvps().getAvp(Avp.CC_REQUEST_NUMBER).getUnsigned32();

            String msisdn = null, imsi = null;

            AvpSet subscripsonIdAvps = diameterRequest.getMessage().getAvps().getAvps(Avp.SUBSCRIPTION_ID);
            if (subscripsonIdAvps != null) {
                Iterator<Avp> it = subscripsonIdAvps.iterator();
                while (it.hasNext()) {
                    Avp next = it.next();
                    // KB
                    Long subscriptionIdTypeCode = next.getGrouped().getAvp(Avp.SUBSCRIPTION_ID_TYPE).getUnsigned32(); // enum....
                    SubscriptionIdType subscriptionIdType = SubscriptionIdType
                            .getCCRequestTypeByCode(subscriptionIdTypeCode);

                    String subscriptionIdData = next.getGrouped().getAvp(Avp.SUBSCRIPTION_ID_DATA).getUTF8String();
                    if (subscriptionIdType.equals(SubscriptionIdType.END_USER_MSISDN)) {
                        msisdn = subscriptionIdData;
                    } else if (subscriptionIdType.equals(SubscriptionIdType.END_USER_IMSI)) {
                        imsi = subscriptionIdData;
                    }
                }
            }

            Long ccServiceSpecificUnits = null;
            if (ccRequestNumber == 0L) { // balance-query
                ccServiceSpecificUnits = diameterRequest.getMessage().getAvps().getAvp(Avp.REQUESTED_SERVICE_UNIT)
                        .getGrouped().getAvp(Avp.CC_SERVICE_SPECIFIC_UNITS).getUnsigned64();
            }
            if (ccRequestNumber == 1L) { // debiting
                ccServiceSpecificUnits = diameterRequest.getMessage().getAvps().getAvp(Avp.USED_SERVICE_UNIT)
                        .getGrouped().getAvp(Avp.CC_SERVICE_SPECIFIC_UNITS).getUnsigned64();
            }

            String smsSubmissionResult = null;
            if (ccServiceSpecificUnits == 0) {
                smsSubmissionResult = "FAILURE";
            } else if (ccServiceSpecificUnits == 1) {
                smsSubmissionResult = "SUCCESS";
            }

            String vlrGt = null, smscAddress = null, tpDestinationAddress = null;
            AvpSet serviceParameterInfos = diameterRequest.getMessage().getAvps().getAvps(Avp.SERVICE_PARAMETER_INFO);
            if (serviceParameterInfos != null) {
                Iterator<Avp> it = serviceParameterInfos.iterator();
                while (it.hasNext()) {
                    Avp next = it.next();
                    Long serviceParameterType = next.getGrouped().getAvp(Avp.SERVICE_PARAMETER_TYPE).getUnsigned32();
                    try {
                        String serviceParameterValue = new String(next.getGrouped().getAvp(Avp.SERVICE_PARAMETER_VALUE)
                                .getOctetString(), "UTF-8");
                        if (serviceParameterType.equals(100L)) {
                            vlrGt = serviceParameterValue;
                        } else if (serviceParameterType.equals(101L)) {
                            smscAddress = serviceParameterValue;
                        } else if (serviceParameterType.equals(102L)) {
                            tpDestinationAddress = serviceParameterValue;
                        }
                    } catch (UnsupportedEncodingException e) {
                        logger.warn(
                                "Could not decode octet string parameter from diameter message: unsupported encoding: {}. Omitting parameters!",
                                e.getMessage(), e);
                    }
                }
            }

            diameterGWCreditControlRequest = new DiameterGWCreditControlRequest();
            diameterGWCreditControlRequest.setCaller(msisdn);
            diameterGWCreditControlRequest.setCallee(tpDestinationAddress);
            diameterGWCreditControlRequest.setRequestType(ccRequestType);
            diameterGWCreditControlRequest.setCallerImsi(imsi);
            diameterGWCreditControlRequest.setVlrGt(vlrGt);
            diameterGWCreditControlRequest.setSmscAddress(smscAddress);
            diameterGWCreditControlRequest.setSmsSubmissionResult(smsSubmissionResult);

            return diameterGWCreditControlRequest;

        } catch (AvpDataException e) {
            logger.warn("AvpDataException: {}. Returning null!", e.getMessage(), e);
        } catch (InternalException e) {
            logger.warn("InternalException: {}. Returning null!", e.getMessage(), e);
        }

        return diameterGWCreditControlRequest;
    }

    public static String getServiceContextIdFromCCR(JCreditControlRequest diameterRequest) {
        try {
            return diameterRequest.getMessage().getAvps().getAvp(Avp.SERVICE_CONTEXT_ID).getUTF8String();
        } catch (AvpDataException e) {
            logger.warn("AvpDataException: {}. Returning null!", e.getMessage(), e);
        } catch (InternalException e) {
            logger.warn("InternalException: {}. Returning null!", e.getMessage(), e);
        }
        return null;
    }

    public static DiameterGWCCASessionData getCCASessionData(ServerCCASession session,
            JCreditControlRequest diameterRequest) {
        logger.debug("Diameter get CCA session data.");

        DiameterGWCCASessionData diameterGWCCASessionData = null;

        try {
            diameterGWCCASessionData = new DiameterGWCCASessionData(session);

            String sessionId = diameterRequest.getMessage().getAvps().getAvp(Avp.SESSION_ID).getUTF8String();
            Long ccRequestTypeCode = diameterRequest.getMessage().getAvps().getAvp(Avp.CC_REQUEST_TYPE).getUnsigned32();
            CCRequestType ccRequestType = CCRequestType.getCCRequestTypeByCode(ccRequestTypeCode);
            Long ccRequestNumber = diameterRequest.getMessage().getAvps().getAvp(Avp.CC_REQUEST_NUMBER).getUnsigned32();

            diameterGWCCASessionData.setSessionId(sessionId);
            diameterGWCCASessionData.setCcRequestType(ccRequestType);
            diameterGWCCASessionData.setCcRequestNumber(ccRequestNumber);
            diameterGWCCASessionData.setRequest(diameterRequest);

        } catch (AvpDataException e) {
            logger.warn("AvpDataException: {}. Returning null!", e.getMessage(), e);
        } catch (InternalException e) {
            logger.warn("InternalException: {}. Returning null!", e.getMessage(), e);
        }

        return diameterGWCCASessionData;
    }

    public static IAnswer createCreditControlAnswerMessage(
            DiameterGWCreditControlResponse diameterGWCreditControlResponse, String sessionId,
            DiameterGWCCASessionData sessionData) {
        logger.debug("Diameter credit control answer message creation.");

        IAnswer message = null;

        try {
            if (sessionId != null && !"".equals(sessionId)) {
                message = (IAnswer) ((MessageImpl) DiameterGW.getDataForCCASessionId().get(sessionId).getRequest()
                        .getMessage()).createAnswer();
                message.setRequest(false);

                CCAResultCode resultCode = diameterGWCreditControlResponse.getQueryResultObject();
                if (resultCode == CCAResultCode.SUCCESS) {
                    message.getAvps().addAvp(Avp.RESULT_CODE, CCAResultCode.SUCCESS.code(), true, false, true);
                } else if (resultCode == CCAResultCode.END_USER_SERVICE_DENIED
                        && sessionData.getCcRequestType() == CCRequestType.BALANCE) {
                    message.getAvps().addAvp(Avp.RESULT_CODE, CCAResultCode.END_USER_SERVICE_DENIED.code(), true,
                            false, true);
                } else if (resultCode == CCAResultCode.TECHNICAL_ERROR) {
                    message.getAvps().addAvp(Avp.RESULT_CODE, CCAResultCode.TECHNICAL_ERROR.code(), true, false, true);
                } else {
                    logger.warn("Diameter credit control answer message creation. Result code: " + resultCode);
                    message.getAvps().addAvp(Avp.RESULT_CODE, 5031, true, false, true);
                }

                if (resultCode != CCAResultCode.SUCCESS) {
                    if (diameterGWCreditControlResponse.getErrorCode() != null) {
                        message.getAvps().addAvp(Avp.ERROR_MESSAGE, diameterGWCreditControlResponse.getErrorCode(),
                                false, false, false);
                    } else {
                        message.getAvps().addAvp(Avp.ERROR_MESSAGE, "", false, false, false);
                    }
                }

                message.getAvps().addAvp(Avp.CC_REQUEST_TYPE, (sessionData.getCcRequestType().code()), true, false,
                        true);
                message.getAvps().addAvp(Avp.CC_REQUEST_NUMBER, sessionData.getCcRequestNumber(), true, false, true);

                ElementParser elementParser = new ElementParser();
                if (sessionData.getCcRequestType() == CCRequestType.BALANCE) { // balance-query
                    if (diameterGWCreditControlResponse.getResponseCaller() != null
                            && !diameterGWCreditControlResponse.getResponseCaller().equals("")) {
                        // set to 0 for the END_USER_MSISDN
                        AvpSet grouped = message.getAvps().addGroupedAvp(SDP_SERVICE_PARAMETER_INFO, RESTCOMM_VENDOR_ID,
                                true, false);
                        grouped.addAvp(Avp.SERVICE_PARAMETER_TYPE, 0L, false, false, true);

                        grouped.addAvp(Avp.SERVICE_PARAMETER_VALUE,
                                elementParser.utf8StringToBytes(diameterGWCreditControlResponse.getResponseCaller()),
                                false, false);
                    }
                    if (diameterGWCreditControlResponse.getResponseCallee() != null
                            && !diameterGWCreditControlResponse.getResponseCallee().equals("")) {
                        // set to 102 for the recipients address
                        AvpSet grouped = message.getAvps().addGroupedAvp(SDP_SERVICE_PARAMETER_INFO, RESTCOMM_VENDOR_ID,
                                true, false);
                        grouped.addAvp(Avp.SERVICE_PARAMETER_TYPE, 102L, false, false, true);

                        grouped.addAvp(Avp.SERVICE_PARAMETER_VALUE,
                                elementParser.utf8StringToBytes(diameterGWCreditControlResponse.getResponseCallee()),
                                false, false);
                    }
                    if (diameterGWCreditControlResponse.getResponseSmscAddress() != null
                            && !diameterGWCreditControlResponse.getResponseSmscAddress().equals("")) {
                        // set to 101 for the SMSC address
                        AvpSet grouped = message.getAvps().addGroupedAvp(SDP_SERVICE_PARAMETER_INFO, RESTCOMM_VENDOR_ID,
                                true, false);
                        grouped.addAvp(Avp.SERVICE_PARAMETER_TYPE, 101L, false, false, true);

                        grouped.addAvp(Avp.SERVICE_PARAMETER_VALUE, elementParser
                                .utf8StringToBytes(diameterGWCreditControlResponse.getResponseSmscAddress()), false,
                                false);
                    }
                }
            }

        } catch (Exception e) {
            logger.warn("Diameter credit control answer message creation. Error: {}", e.getMessage(), e);
        }
        return message;
    }
}
