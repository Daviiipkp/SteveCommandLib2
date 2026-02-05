package com.daviipkp.stevecommandlib2.info;

import com.daviipkp.stevecommandlib2.Jsoning;
import com.daviipkp.stevecommandlib2.instance.InfoCommand;

public class InfoStack {

    private String[] keys;
    private String[] values;

    public InfoStack(String... keyList) {
        keys = keyList;
        values = new String[keyList.length];
    }

    public String getJsonObject() {
        StringBuilder b = new StringBuilder();
        b.append("{").append(Jsoning.lineBreak());
        int i = 0;
        for(String s : keys) {
            b.append( Jsoning.formattedLine(s, values[i], 1, ( i == (keys.length-1) ),true) );
            i++;
        }
        b.append("}");
        return b.toString();
    }

    public InfoStack fromJson(String json) {
        return null;
    }

    public void fulfillKey(String key, InfoCommand value) {
        int i = 0;
        for(String s : keys) {
            if(key.equals(s)) {
                values[i] = value.getReturn();
                return;
            }
            i++;
        }
        throw new IllegalArgumentException("Key " + key + " wasn't found. Check if you're adding it in the constructor of the InfoStack.");
    }

    public void fulfillKey(String key, String value) {
        int i = 0;
        for(String s : keys) {
            if(key.equals(s)) {
                values[i] = value;
                return;
            }
            i++;
        }
        throw new IllegalArgumentException("Key " + key + " wasn't found. Check if you're adding it in the constructor of the InfoStack.");
    }

    public void fulfillEmpty(String value) {
        for(int i = 0; i < values.length; i++) {
            if(values[i].isEmpty() || values[i] == null) {
                values[i] = value;
            }
        }
    }
}
