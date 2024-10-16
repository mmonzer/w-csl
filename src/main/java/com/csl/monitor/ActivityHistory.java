package com.csl.monitor;

import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;

import java.util.LinkedList;
import java.util.List;

public class ActivityHistory {
    /**
     * Contains the recent history of the activity, to be displayed when opening the dashboard in the HMI
     * /!\ Currently not thread-safe!
     */
    private int history_size = 10;

    private List<Integer> histogram = new LinkedList<>();
    private List<Integer> links = new LinkedList<>();
    private List<Long> times = new LinkedList<>();

    ActivityHistory(int max_size) {
        history_size = max_size;
    }

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

    public int currentHistorySize() {
        /**
         * Get the number of elements currently in the history
         *
         * @return the size of the history
         */
        return histogram.size();
    }
    public int maxHistorySize() {
        /**
         * Get the current maximum size of the history
         *
         * @return The current maximum size of the history
         */
        return history_size;
    }

    public void setHistorySize(int size) {
        /**
         * Change the maximum history size
         *
         * @param size The new maximum history size
         */
        this.history_size = Math.max(0, size);
    }

    public Json toJson() {
        /**
         * Returns {@link Json} object ready to be sent to the HMI
         * /!\ Currently not thread-safe!
         *
         * @return a {@link Json} object
         */

        Json res = Json.object();

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

    private <T> List<T> push_back(List<T> array, T value) {
        array.add(value);
        if (array.size() > history_size) {
            array.remove(0);
        }
        return array;
    }
}
