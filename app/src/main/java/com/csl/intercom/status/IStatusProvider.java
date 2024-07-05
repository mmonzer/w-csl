package com.csl.intercom.status;

import com.ucsl.interfaces.ICSLService;
import com.ucsl.json.Json;

/**
 * Interface for classes that provide a way to get status information.
 */
public interface IStatusProvider {
    /**
     * Retrieve the status of an object.
     *
     * @return The status, in a {@link Json} object. The fields should be in lower case with underscores to separate words,
     *         and provide booleans whenever possible (eg. <code>"is_running": true</code>).
     */
    public Json getStatus();
}
