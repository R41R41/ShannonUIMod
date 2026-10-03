package com.shannon.sync;

/**
 * A named, typed request sent from the client to the server.
 *
 * @param name the wire name, unique among actions
 * @param type the request class, encoded as JSON
 */
public record ActionChannel<T>(String name, Class<T> type) {
}
