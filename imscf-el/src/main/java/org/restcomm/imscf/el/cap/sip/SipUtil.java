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
package org.restcomm.imscf.el.cap.sip;

import org.restcomm.imscf.common.config.SipApplicationServerType;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.config.ConfigBean;
import org.restcomm.imscf.el.sip.SIPCall;
import org.restcomm.imscf.el.sip.servlets.MainServlet;
import org.restcomm.imscf.el.sip.servlets.SipServletResources;
import org.restcomm.imscf.el.sip.servlets.SipSessions;
import org.restcomm.imscf.el.stack.CallContext;
import org.restcomm.imscf.util.IteratorStream;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.sip.Rel100Exception;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.SipSession.State;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for CAP operations on SIP.
 */
@SuppressWarnings("PMD.GodClass")
public final class SipUtil {

    private static final Logger LOG = LoggerFactory.getLogger(SipUtil.class);

    private static final Pattern NO_CONNECTION_SDP_PATTERN = Pattern.compile("^\\s*" // allow preamble whitespace
            + "(?:v=0\r?\n)?" // allow but ignore mandatory version 0
            + "(?:o=.*\r?\n)?" // allow but ignore mandatory originator and session id
            + "(?:s=.*\r?\n)?" // allow but ignore mandatory session name
            + "i=0\r?\n" // require exactly i=0
            // don't allow u=, e= and p= lines
            + "c=IN IP4 0.0.0.0\r?\n" // require exactly c=IN IP4 0.0.0.0
            + "\\s*" // allow but ignore more whitespace, but nothing else
    );

    private static final Pattern LEG_MANIPULATION_SDP_PATTERN = Pattern.compile("^\\s*" // allow preamble whitespace
            + "(?:v=0\r?\n)?" // allow but ignore mandatory version 0
            + "(?:o=.*\r?\n)?" // allow but ignore mandatory originator and session id
            + "(?:s=.*\r?\n)?" // allow but ignore mandatory session name
            + "i=(.*)\r?\n" // require and match group i=...
            + "\\s*" // allow but ignore more whitespace, but nothing else
    );

    // token = 1*(alphanum / "-" / "." / "!" / "%" / "*" / "_" / "+" / "`" / "'" / "~" )
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[-.!%*_+`'~A-Za-z0-9]+");

    private SipUtil() {
        // NOOP
    }

    public static void prepareInitialInviteToAS(SipServletRequest invite, SipURI appServerURI) {
        ConfigBean configBean = Objects.requireNonNull((ConfigBean) CallContext.get(CallContext.CONFIG),
                "ConfigBean missing from CallContext");
        String key = MainServlet.getAppSessionKey(invite.getApplicationSession());
        invite.pushRoute(MainServlet.prepareBackRouteURI(configBean.getLocalSipURI(), key));
        invite.pushRoute(appServerURI);
    }

    public static void prepareIcaResponseToAS(SipServletResponse resp) {
        ConfigBean configBean = Objects.requireNonNull(CallContext.getConfigBean(),
                "ConfigBean missing from CallContext");
        String key = MainServlet.getAppSessionKey(resp.getApplicationSession());
        SipURI uri = MainServlet.prepareBackRouteURI(configBean.getLocalSipURI(), key);
        resp.setHeader(SipConstants.HEADER_ICA_ROUTE, uri.toString());
    }

    public static String createSdpForLegs(SipSession... legs) {
        String iLine = Arrays.stream(legs).map(ss -> SipSessionAttributes.LEG_ID.get(ss, String.class))
                .collect(Collectors.joining(","));
        return "v=0\r\no=IMSCF\r\ni=" + iLine + "\r\n";
    }

    public static String createSdpForMrfLegs(SipSession... legs) {
        String iLine = Arrays.stream(legs).map(ss -> SipSessionAttributes.LEG_ID.get(ss, String.class))
                .collect(Collectors.joining(","));
        String sLine = Arrays.stream(legs).map(ss -> SipSessionAttributes.MRF_ALIAS.get(ss, String.class))
                .collect(Collectors.joining(","));
        return "v=0\r\no=IMSCF\r\ns=" + sLine + "\r\ni=" + iLine + "\r\n";
    }

    public static boolean isNoConnectionSdp(String sdp) {
        return NO_CONNECTION_SDP_PATTERN.matcher(sdp).matches();
    }

    public static List<String> getLegIDListFromSdp(String sdp) {
        Matcher m = LEG_MANIPULATION_SDP_PATTERN.matcher(sdp);
        if (m.matches()) {
            return Arrays.asList(m.group(1).split(",")).stream().map(String::trim).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public static SipURI createAppServerRoutingAddress(SipApplicationServerType appServer) {
        SipURI asRoute = SipServletResources.getSipFactory().createSipURI(appServer.getName(), appServer.getHost());
        asRoute.setPort(appServer.getPort());
        asRoute.setTransportParam("udp");
        asRoute.setLrParam(true);
        return asRoute;
    }

    /** Returns the first active (i.e. not "{@link #isUaTerminated(SipSession) UA terminated}") SIP session with the corresponding legID,
     *  or <code>null</code> if none is found.
     *  <p>
     *  Note: UA terminated sessions are filtered out to allow recreation of sessions
     *  with the same ID while previously terminated sessions still linger in the
     *  SipApplicationSession. */
    public static SipSession findSipSessionForLegID(SIPCall call, String legID) {
        Objects.requireNonNull(call, "Call is null");
        Objects.requireNonNull(legID, "legID is null");
        SipSession ret = IteratorStream
                .of(SipSessions.of(call.getAppSession()))
                .filter(s -> !SipUtil.isUaTerminated(s)
                        && legID.equals(SipSessionAttributes.LEG_ID.get(s, String.class))).findFirst().orElse(null);
        LOG.trace("SipSession for {}: {}", legID, ret);
        return ret;
    }

    public static boolean isReliable(SipServletResponse resp) {
        return IteratorStream.of(resp.getHeaders("Require")).anyMatch(s -> "100rel".equals(s));
    }

    /**
     * Sends the message or warns if it fails.
     * @return true if the message was sent
     */
    public static boolean sendOrWarn(SipServletMessage msg, String warnLog, Object... params) {
        try {
            msg.send();
            return true;
        } catch (IOException e) {
            LOG.warn(Optional.ofNullable(warnLog).orElse("Failed to send message"), params, e);
            return false;
        }
    }

    public static boolean sendReliablyOrWarn(SipServletResponse resp, String warnLog, Object... params) {
        try {
            resp.sendReliably();
            return true;
        } catch (Rel100Exception e) {
            LOG.warn(Optional.ofNullable(warnLog).orElse("Failed to send reliable response"), params, e);
            return false;
        }
    }

    /**
     * Check whether a session is terminated from the UA's point of view, meaning one of the following conditions holds:
     * <ul>
     * <li>the SipSession is already {@link #isTerminated(SipSession) container terminated}; or</li>
     * <li>a disconnect message (CANCEL or BYE) was sent already; or</li>
     * <li>a non-success final response was already received for the initial outgoing INVITE. (note: forking is not allowed downstream,
     *  so while the SipSession returns to {@link State#INITIAL INITIAL} state in this case, it is effectively terminated for us.)</li>
     * </ul>
     * In this state, no more requests can be sent by the UA, but responses may still arrive for the disconnect message or the initial INVITE.
     * Note that the transaction layer may still send ACK requests to final responses to the INITIAL INVITE.
     */
    public static boolean isUaTerminated(SipSession s) {
        String name = s != null && s.isValid() ? SipSessionAttributes.LEG_ID.get(s, String.class) : "N/A";

        // this will return true for UAC/UAS initiated dialogs, if the dialog is terminated on the
        // container level
        if (isTerminated(s)) {
            LOG.trace("isUaTerminated({}): true, isTerminated()", name);
            return true;
        }

        // if not terminated on the container level, check whether we are already disconnecting, which makes it
        // impossible to send further client requests
        if (SipSessionAttributes.UAC_DISCONNECTED.get(s, Object.class) != null) {
            LOG.trace("isUaTerminated({}): true, UAC_DISCONNECTED is set", name);
            return true;
        }

        // for dialogs started as INVITE UAC, 4xx-6xx returns the state to INITIAL instead of TERMINATED, so check for
        // that too, because isReadyToInvalidate() will still return false for another 32 seconds...
        if (s.getState() == State.INITIAL) {
            int status = Optional.ofNullable(SipSessionAttributes.INITIAL_RESPONSE.get(s, SipServletResponse.class))
                    .map(SipServletResponse::getStatus).orElse(-1);
            if (status >= 300) {
                LOG.trace("isUaTerminated({}): true, INITIAL && {} response", name, status);
                return true;
            }
        }

        LOG.trace("isUaTerminated({}): false", name);
        return false;
    }

    /**
     * Checks whether a session is terminated from the container's point of view, meaning the SipSession is:
     * <ul>
     * <li><code>null</code>; or</li>
     * <li>{@link SipSession#isValid() invalid}; or</li>
     * <li>{@link SipSession#isReadyToInvalidate() ready to invalidate}; or</li>
     * <li>in {@link State#TERMINATED TERMINATED} state.</li>
     * </ul>
     * In this state, no more messages can be sent or received in this SipSession.
     */
    public static boolean isTerminated(SipSession s) {
        if (s == null) {
            LOG.trace("isTerminated(null): true, null");
            return true;
        }

        String name = s.isValid() ? SipSessionAttributes.LEG_ID.get(s, String.class) : "N/A";

        if (!s.isValid()) {
            LOG.trace("isTerminated({}): true, invalid", name);
            return true;
        } else if (s.getState() == State.TERMINATED) {
            LOG.trace("isTerminated({}): true, TERMINATED", name);
            return true;
        } else if (s.isReadyToInvalidate()) {
            LOG.trace("isTerminated({}): true, readyToInvalidate", name);
            return true;
        }

        LOG.trace("isTerminated({}): false, state={}", name, s.getState());
        return false;
    }

    public static SipServletRequest getInitialRequest(SipSession s) {
        return SipSessionAttributes.INITIAL_REQUEST.get(s, SipServletRequest.class);
    }

    public static SipServletRequest getInitialRequest(SipServletMessage s) {
        return getInitialRequest(s.getSession());
    }

    public static boolean isErrorResponseForInitialInvite(SipServletMessage msg) {
        if (msg instanceof SipServletResponse && "INVITE".equals(msg.getMethod())) {
            SipServletResponse resp = (SipServletResponse) msg;
            return resp.getStatus() >= 400 && resp.getRequest().equals(getInitialRequest(resp));
        } else {
            return false;
        }
    }

    /** Same as {@link SipServletMessage#isCommitted()}, except that ACK requests are always considered committed. */
    public static boolean isCommitted(SipServletMessage msg) {
        if ("ACK".equals(msg.getMethod()))
            return true;
        return msg.isCommitted();
    }

    public static boolean supports100Rel(SipServletRequest invite) {
        return IteratorStream.of(invite.getHeaders("Supported")).anyMatch(s -> "100rel".equals(s));
    }

    public static int networkLegIdFromSdpId(String sdpId) {
        return Integer.parseInt(sdpId.substring(1));
    }

    public static String sdpIdFromNetworkLegId(int networkLegId) {
        return "L" + networkLegId;
    }

    /**
     * Returns value without quotes for quoted strings, value itself for tokens, null otherwise.
     */
    public static String unQuote(String value) {
        if (value == null)
            return null;
        if (QuotedString.isValidQuotedString(value))
            return QuotedString.toUnescapedValue(value, false);
        if (isValidToken(value))
            return value;
        return null;
    }

    // public static void main(String[] args) {
    // String val = "hello\u0002\\nice\r\n w\"orld";
    // System.out.println(QuotedString.toQuotedString(val, true));
    // System.out.println("unquote(quote) OK? " + val.equals(unQuote(QuotedString.toQuotedString(val, true))));
    // System.out.println();
    // System.out.println(createWarningHeader("Message: \"Houston we have a problem!\""));
    // }

    /**
     * Returns true if value is a string that matches the 'token' definition in SIP RFC3261.
     */
    public static boolean isValidToken(String value) {
        return value != null && TOKEN_PATTERN.matcher(value).matches();
    }

    /** Returns the CSeq of the message.
     * <p>
     * Note: The result may be a negative number, as the value is parsed as a 32bit unsigned number. This is not an error. */
    public static int getCSeq(SipServletMessage msg) {
        // format is as below, with the number being a max 32 bit unsigned int
        // CSeq: 2 INVITE
        return Integer.parseUnsignedInt(msg.getHeader("CSeq").split(" ")[0]);
    }

    public static SipSession findMrfSessionForCallSegment(CapSipCsCall call, int callSegmentId) {
        // multiple call segments could each have their own MRF, all with the LEG_ID "mrf". Find the correct one.
        return IteratorStream
                .of(call.getSipSessions())
                .filter(s -> s.isValid() && !s.isReadyToInvalidate()
                        && "mrf".equals(SipSessionAttributes.LEG_ID.get(s, String.class))
                        && Integer.valueOf(callSegmentId).equals(SipSessionAttributes.MRF_CS_ID.get(s, Integer.class)))
                .findFirst().orElse(null);
    }

    /** Calls {@link #createWarningHeader(String)} and sets the result in the message.
     * @return the same SipServletMessage */
    public static <T extends SipServletMessage> T createAndSetWarningHeader(T msg, String warning) {
        msg.setHeader("Warning", createWarningHeader(warning));
        return msg;
    }

    /** Creates an IMSCF Warning header value with the specified message. The message is automatically converted
     *  to <code>quoted-string</code> form as required by the SIP RFC if it is not already a valid quoted string
     *  (including the start-end quotes). If a conversion occurs, all contained " and \ characters will be
     *  escaped, and the value surrounded by double quotes.
     *  <p>For example,
     * <pre>createWarningHeader("hello \"world\"!");</pre>
     * returns<br/>
     * <pre>399 imscf.appngin.alerant.hu "hello \"world\"!"</pre>
     * .*/
    public static String createWarningHeader(String message) {
        // Warning header has a strict format: 3 digit code, space, host:port or pseudonym token, space, quoted-string
        // message
        return QuotedString.appendAsQuotedString(new StringBuffer(SipConstants.WARNING_HEADER_VALUE_START), message,
                true).toString();
    }

}
