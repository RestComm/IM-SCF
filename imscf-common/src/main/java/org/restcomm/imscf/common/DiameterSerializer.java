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

import org.restcomm.imscf.common .diameter.creditcontrol.DiameterSLELCreditControlRequest;
import org.restcomm.imscf.common .diameter.creditcontrol.DiameterSLELCreditControlResponse;

import java.lang.reflect.Type;

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

/**
 *  Class for (de)serializing DiameterSLELCreditControlRequest and DiameterSLELCreditControlResponse objects to/from JSON.
 */
public final class DiameterSerializer {
    private DiameterSerializer() {

    }

    private static final ThreadLocal<Gson> GSON = new ThreadLocal<Gson>() {
        @Override
        protected Gson initialValue() {

            ExclusionStrategy exc = new ExclusionStrategy() {
                @Override
                public boolean shouldSkipClass(Class<?> arg0) {
                    // if (SccpStackImpl.class.isAssignableFrom(arg0)) {
                    // return true;
                    // }
                    return false;
                }

                @Override
                public boolean shouldSkipField(FieldAttributes arg0) {
                    return false;
                }
            };
            return new GsonBuilder().setExclusionStrategies(exc).create();

            // return new GsonBuilder().setExclusionStrategies(exc)
            // .registerTypeAdapter(ProtocolClass.class, new DiameterInterfaceAdapter<ProtocolClass>())
            // .registerTypeAdapter(GlobalTitle.class, new DiameterInterfaceAdapter<GlobalTitle>()).create();
        }
    };

    public static String serialize(DiameterSLELCreditControlRequest msg) {
        return GSON.get().toJson(msg);
    }

    public static DiameterSLELCreditControlRequest deserializeRequest(String source) {
        DiameterSLELCreditControlRequest sdm = GSON.get().fromJson(source, DiameterSLELCreditControlRequest.class);

        return sdm;
    }

    public static String serialize(DiameterSLELCreditControlResponse msg) {
        return GSON.get().toJson(msg);
    }

    public static DiameterSLELCreditControlResponse deserializeResponse(String source) {
        DiameterSLELCreditControlResponse sdm = GSON.get().fromJson(source, DiameterSLELCreditControlResponse.class);

        return sdm;
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
final class DiameterInterfaceAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T> {
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
