package com.shannon.sync;

/**
 * A named, typed stream of state sent from the server to the client.
 *
 * @param name the wire name, unique among channels
 * @param type the state class, encoded as JSON
 */
public record SyncChannel<T>(String name, Class<T> type) {
}
