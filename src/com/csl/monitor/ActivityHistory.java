package com.csl.monitor;

import com.csl.core.CSLContext;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;

import java.util.ArrayList;

public class ActivityHistory {
    /**
     * Contains the recent history of the activity, to be displayed when opening the dashboard in the HMI
     * /!\ Currently not thread-safe!
     */
    private int history_size = 10;

    private ArrayList<Integer> histogram = new ArrayList<>(history_size+1);
    private ArrayList<Integer> links = new ArrayList<>(history_size+1);
    private ArrayList<Long> times = new ArrayList<>(history_size+1);

    ActivityHistory(int max_size) {
        history_size = max_size;
    }

    ActivityHistory() {}

    public void addTick(int nb_packets, int nb_links, long time) {
        /**
         * Registers a tick in the history
         * /!\ Currently not thread safe!
         *
         * @param nb_packets The number of packets in the tick
         * @param nb_links The number of links in the tick
         * @param time The timestamp of the tick
         */

        histogram = push_back(histogram, nb_packets);
        links = push_back(links, nb_links);
        times = push_back(times, time);
    }

    public void addTick(Json tick) {
        /**
         * Adds a tick in the history from the JSON object representing it.
         *
         * @param tick a {@link Json} object representing the tick
         */
        int nb_packets = JsonUtil.getIntFromJson(tick, "nb_packets", 0);
        int nb_links = 0;
        for (Json tap: tick.get("taps").asJsonList()) {
            nb_links += JsonUtil.getIntFromJson(tap, "nb_packets", 0);
        }
        long time = JsonUtil.getLongFromJson(tick, "timestamp", System.currentTimeMillis());
        addTick(nb_packets, nb_links, time);
    }

    public Json toJson() {
        /**
         * Returns {@link Json} object ready to be sent to the HMI
         * /!\ Currently not thread-safe!
         *
         * @return a {@link Json} object
         */

        Json res = Json.object();
//        int nb_links = 0;
//        for (int nb: links) {
//            nb_links += nb;
//        }
//        res.set("nb_links", nb_links);

        Json jHisto = Json.array();
        Json jTimes = Json.array();

        for (int i = 0; i < Math.min(histogram.size(), times.size()); ++i) {
            jHisto.add(histogram.get(i));
            jTimes.add(times.get(i));
        }
        res.set("histo", jHisto);
        res.set("times", jTimes);

        res.set("period", getPeriod());

        return res;
    }

    private long getPeriod() {
        double sum = 0;
        for (int i = 0; i < times.size() - 1; ++i) {
            sum += times.get(i+1) - times.get(i);
        }
        return Math.round(sum/(1000*(times.size()-1)));
    }

    private <T> ArrayList<T> push_back(ArrayList<T> array, T value) {
        ArrayList<T> res = new ArrayList<>(array);
        res.add(value);
        if (res.size() > history_size) {
            res.remove(0);
        }
        return res;
    }
}
