package com.ucsl.interfaces;

import com.ucsl.json.Json;

/**
 * Implementations are service commands.
 */
@FunctionalInterface
public interface IJsonCmd {

	public Json exec(Json params);

}
