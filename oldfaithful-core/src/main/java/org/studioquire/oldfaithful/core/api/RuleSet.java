package org.studioquire.oldfaithful.core.api;

/**
 * Rules of the world: owned by whoever runs it, kept in the world's folder, read-only to a client on a server. Labels
 * come from the mod's lang file: {@code rule.<mod id>.<key>.name} and {@code .hint}.
 */
public interface RuleSet {
	/** A rule that's on or off; the default is beta's behaviour, so a world nobody touched plays as it did. */
	BoolRule bool(String key, boolean defaultValue);

	/**
	 * A rule of named values: lowercase words, which the world's file and {@code /rule} use, the first of them beta's behaviour and the default. Each has a word for the screen in the lang file,
	 * {@code rule.<mod id>.<key>.<value>}.
	 */
	ChoiceRule choice(String key, String... values);

	/**
	 * The same, for rules a world chooses once, when it's created, and keeps: for what a world is made of rather than how it plays, such as how its generated buildings look, where changing it
	 * partway would leave the world half one thing and half the other. Set on Create World's World Rules page, or on a server in the world's file before its first start; shown inside a world, but
	 * not changed there, nor by {@code /rule}. A world made before the rule existed has the default.
	 */
	RuleSet atCreation();
}
