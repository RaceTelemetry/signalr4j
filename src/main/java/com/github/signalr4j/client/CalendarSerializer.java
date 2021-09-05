/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Date;

public class CalendarSerializer implements JsonSerializer<Calendar>, JsonDeserializer<Calendar> {

    private static final DateSerializer INTERNAL_SERIALIZER = new DateSerializer();

    @Override
    public Calendar deserialize(JsonElement element, Type type, JsonDeserializationContext ctx) throws JsonParseException {
        Date date = INTERNAL_SERIALIZER.deserialize(element, Date.class, ctx);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return calendar;
    }

    @Override
    public JsonElement serialize(Calendar calendar, Type type, JsonSerializationContext ctx) {
        return INTERNAL_SERIALIZER.serialize(calendar.getTime(), Date.class, ctx);
    }

}
