/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Date Serializer/Deserializer to make .NET and Java dates compatible
 */
public class DateSerializer implements JsonSerializer<Date>, JsonDeserializer<Date> {

    private static final int THREE_MILLISECONDS_DATE_FORMAT_LENGTH = 29;

    /**
     * Deserializes a JsonElement containing an ISO-8601 formatted date
     */
    @Override
    public Date deserialize(JsonElement element, Type type, JsonDeserializationContext ctx) throws JsonParseException {
        String strVal = element.getAsString();

        try {
            return deserialize(strVal);
        } catch (ParseException e) {
            throw new JsonParseException(e);
        }
    }

    /**
     * Serializes a Date to a JsonElement containing a ISO-8601 formatted date
     */
    @Override
    public JsonElement serialize(Date date, Type type, JsonSerializationContext ctx) {
        return new JsonPrimitive(serialize(date));
    }

    /**
     * Deserializes an ISO-8601 formatted date
     */
    public static Date deserialize(String strVal) throws ParseException {
        
    	String s;
    	
    	if(strVal.contains("+00:00")){
    		strVal = strVal.replace("+00:00", "");
    	}
    	
    	if(strVal.length() == 19){// adapt from format yyyy-MM-ddTHH:mm:dd
    		s = strVal + ".+00:00";
    	}else if(strVal.contains(".Z")){
    		// Change .Z to +00:00 to adapt the string to a format
            // that can be parsed in Java
    		s = strVal.replace(".Z", ".+00:00");
    	}else{ 
    		// Change Z to +00:00 to adapt the string to a format
            // that can be parsed in Java
    		s = strVal.replace("Z", ".+00:00");
    	}
    	     
        try {
            // Remove the ":" character to adapt the string to a
            // format
            // that can be parsed in Java

            String part = s.substring(s.length() - 6);
            if (s.length() > THREE_MILLISECONDS_DATE_FORMAT_LENGTH) { // yyyy-MM-ddTHH:mm:dd.SSS+00:00
                // remove the extra milliseconds characters
                s = s.substring(0, 23) + part;
            } else if (s.length() < THREE_MILLISECONDS_DATE_FORMAT_LENGTH) {
                // add extra milliseconds characters
                int dif = (THREE_MILLISECONDS_DATE_FORMAT_LENGTH - s.length());

                StringBuilder zeros = new StringBuilder();
                for (int i = 0; i < dif; i++) {
                    zeros.append("0");
                }
                s = s.substring(0, 20 + (3 - dif)) + zeros + part;
            }

            s = s.substring(0, 26) + s.substring(27);
        } catch (IndexOutOfBoundsException e) {
            throw new JsonParseException("Invalid length for: " + s);
        }

        // Parse the well-formatted date string
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'SSSZ");
        dateFormat.setTimeZone(TimeZone.getDefault());

        return dateFormat.parse(s);
    }

    /**
     * Serializes a Date object to an ISO-8601 formatted date string
     */
    public static String serialize(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'SSS'Z'", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        return dateFormat.format(date);
    }

}
