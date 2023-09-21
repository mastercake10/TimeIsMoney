package de.Linus122.TimeIsMoney;

import java.util.ArrayList;
import java.util.List;

/**
 * Payout information.
 *
 * @author Linus122
 * @since 1.9.6.1
 */
class Payout {
	int id;
	/**
	 * The payout amount.
	 */
	double payout_amount = 0;
	/**
	 * The max payout per day.
	 */
	double max_payout_per_day = 0;
	/**
	 * The permission to require for the payout, blank if none.
	 */
	String permission = "";
	/**
	 * The chance of getting the payout.
	 */
	double chance = 0;
	/**
	 * The list of commands to execute if this payout is earned.
	 */
	List<String> commands = new ArrayList<>();
	/**
	 * The list of commands to execute if this payout is earned while afk.
	 */
	List<String> commands_if_afk = new ArrayList<>();

	/**
	 * Interval seconds
	 */
	int interval = 0;
}
