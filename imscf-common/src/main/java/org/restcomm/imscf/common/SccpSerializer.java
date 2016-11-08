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
package org.restcomm.imscf.common ;

import java.io.IOException;
import java.lang.reflect.Type;

import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.NumberingPlan;
import org.mobicents.protocols.ss7.indicator.RoutingIndicator;
import org.mobicents.protocols.ss7.sccp.impl.SccpStackImpl;
import org.mobicents.protocols.ss7.sccp.impl.message.MessageFactoryImpl;
import org.mobicents.protocols.ss7.sccp.impl.message.SccpDataMessageImpl;
import org.mobicents.protocols.ss7.sccp.impl.parameter.HopCounterImpl;
import org.mobicents.protocols.ss7.sccp.impl.parameter.ImportanceImpl;
import org.mobicents.protocols.ss7.sccp.message.SccpDataMessage;
import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle;
import org.mobicents.protocols.ss7.sccp.parameter.ProtocolClass;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/** Class for (de)serializing SccpDataMessage objects to/from JSON. */
public final class SccpSerializer {
    private SccpSerializer() {
    }

    private static final ThreadLocal<Gson> GSON = new ThreadLocal<Gson>() {
        @Override
        protected Gson initialValue() {

            ExclusionStrategy exc = new ExclusionStrategy() {
                @Override
                public boolean shouldSkipClass(Class<?> arg0) {
                    if (SccpStackImpl.class.isAssignableFrom(arg0)) {
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean shouldSkipField(FieldAttributes arg0) {
                    return false;
                }
            };

            return new GsonBuilder().setExclusionStrategies(exc)
                    .registerTypeAdapter(ProtocolClass.class, new InterfaceAdapter<ProtocolClass>())
                    .registerTypeAdapter(GlobalTitle.class, new InterfaceAdapter<GlobalTitle>()).create();
        }
    };

    public static String serialize(SccpDataMessage msg) {
        return GSON.get().toJson(msg);
    }

    public static SccpDataMessage deserialize(String source) {
        SccpDataMessageImpl sdm = GSON.get().fromJson(source, SccpDataMessageImpl.class);

        return sdm;
    }

    public static void main(String[] args) throws IOException {

        SccpDataMessage sdm = new MessageFactoryImpl(new SccpStackImpl("name")).createDataMessageClass1(
                new SccpAddress(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, 280, GlobalTitle.getInstance(0,
                        NumberingPlan.ISDN_TELEPHONY, NatureOfAddress.INTERNATIONAL, "363012345678"), 123),
                new SccpAddress(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, 321, GlobalTitle.getInstance(0,
                        NumberingPlan.ISDN_TELEPHONY, NatureOfAddress.INTERNATIONAL, "363015555585"), 44), new byte[] {
                        (byte) 12, (byte) 34 }, 1, 146, false, new HopCounterImpl(2), new ImportanceImpl((byte) 1));

        String sdmString = SccpSerializer.serialize(sdm);
        SccpDataMessage back = SccpSerializer.deserialize(sdmString);
        String again = SccpSerializer.serialize(back);
        System.out.println(sdm);
        System.out.println(back);
        System.out.println(sdmString);
        System.out.println(again);
        System.out.println(sdmString.equals(again) ? "equals" : "nope");

        String elresponse = "{\"protocolClass\":{\"type\":\"org.mobicents.protocols.ss7.sccp.impl.parameter.ProtocolClassImpl\",\"data\":{\"pClass\":1,\"msgHandling\":0}},\"data\":[100,6,73,4,1,2,3,12],\"isFullyRecieved\":true,\"remainingSegments\":0,\"calle\n"
                + "dParty\":{\"gt\":{\"type\":\"org.mobicents.protocols.ss7.sccp.parameter.GT0100\",\"data\":{\"tt\":0,\"np\":\"ISDN_TELEPHONY\",\"encodingScheme\":\"BCD_ODD\",\"nai\":\"INTERNATIONAL\",\"digits\":\"36309879050\"}},\"pc\":0,\"ssn\":146,\"ai\":{\"g\n"
                + "lobalTitleIndicator\":\"GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_NUMBERING_PLAN_ENCODING_SCHEME_AND_NATURE_OF_ADDRESS\",\"pcPresent\":false,\"ssnPresent\":true,\"routingIndicator\":\"ROUTING_BASED_ON_GLOBAL_TITLE\"},\"transl\n"
                + "ated\":false},\"callingParty\":{\"gt\":{\"type\":\"org.mobicents.protocols.ss7.sccp.parameter.GT0100\",\"data\":{\"tt\":0,\"np\":\"ISDN_TELEPHONY\",\"encodingScheme\":\"BCD_ODD\",\"nai\":\"INTERNATIONAL\",\"digits\":\"36309879054\"}},\"pc\":\n"
                + "0,\"ssn\":146,\"ai\":{\"globalTitleIndicator\":\"GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_NUMBERING_PLAN_ENCODING_SCHEME_AND_NATURE_OF_ADDRESS\",\"pcPresent\":false,\"ssnPresent\":true,\"routingIndicator\":\"ROUTING_BASED_ON_DP\n"
                + "C_AND_SSN\"},\"translated\":false},\"isMtpOriginated\":false,\"type\":-1,\"localOriginSsn\":146,\"incomingOpc\":-1,\"incomingDpc\":-1,\"sls\":0,\"outgoingDpc\":-1}";

        sdm = SccpSerializer.deserialize(elresponse);
        System.out.println(sdm);
    }
}

/**
 * Adapter class for reading/writing interface references to/from JSON while keeping the original runtime implementation class.
 * <p>
 * Note: this is required for cases where an object has a member declared with an interface type. During deserialization, Gson
 * cannot determine what class to instantiate that implements this interface. This wrapper writes the runtime class of the
 * object instance the reference is pointing to, and instantiates an object of the same class during deserialization.
 * </p>
 * @param <T> The interface type.
 */
final class InterfaceAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T> {
    @Override
    public JsonElement serialize(T object, Type interfaceType, JsonSerializationContext context) {
        final JsonObject wrapper = new JsonObject();
        wrapper.addProperty("type", object.getClass().getName());
        wrapper.add("data", context.serialize(object));
        return wrapper;
    }

    @Override
    public T deserialize(JsonElement elem, Type interfaceType, JsonDeserializationContext context)
            throws JsonParseException {
        final JsonObject wrapper = (JsonObject) elem;
        final JsonElement typeName = get(wrapper, "type");
        final JsonElement data = get(wrapper, "data");
        final Type actualType = typeForName(typeName);
        return context.deserialize(data, actualType);
    }

    private Type typeForName(final JsonElement typeElem) {
        try {
            return Class.forName(typeElem.getAsString());
        } catch (ClassNotFoundException e) {
            throw new JsonParseException(e);
        }
    }

    private JsonElement get(final JsonObject wrapper, String memberName) {
        final JsonElement elem = wrapper.get(memberName);
        if (elem == null)
            throw new JsonParseException("no '" + memberName
                    + "' member found in what was expected to be an interface wrapper");
        return elem;
    }
}
