package org.studioquire.faithful.stagehand;

/** A script's own trouble, said back to it: bad input, or something it asked for that can't be done. */
final class Failure extends RuntimeException {
	final String code;

	Failure(String code, String message) {
		super(message, null, false, false);
		this.code = code;
	}
}
