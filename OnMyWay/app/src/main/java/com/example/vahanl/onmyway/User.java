package com.example.vahanl.onmyway;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.maps.model.DirectionsResult;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vahanl on 9/25/17.
 */

@IgnoreExtraProperties
public class User {
    public String id;
    public String name;
    public long startTime;
    public int intervalInMinutes;
    public String startLocation;
    public String endLocation;
    public String type;

    public User() {

    }

    public User(String id,
                String name,
                long startTime,
                int intervalInMinutes,
                String startLocation,
                String endLocation,
                String type) {
        this.id = id;
        this.name = name;
        this.startTime = startTime;
        this.intervalInMinutes = intervalInMinutes;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.type = type;
    }


    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("name", name);
        result.put("startTime", startTime);
        result.put("intervalInMinutes", intervalInMinutes);
        result.put("startLocation", startLocation);
        result.put("endLocation", endLocation);
        result.put("type", type);

        return result;
    }

    public boolean isFooter() {
        return type.equals(Constants.TYPE_FOOTER);
    }
}
