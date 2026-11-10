package org.studioquire.faithful.buriedtowers.plan;

import java.util.Arrays;

/** First in, first out, of cells packed as ints: the flood fills' queue, without an object for each cell. */
final class IntQueue {
	private int[] items = new int[4096];
	private int head, tail;

	void add(int value) {
		if (tail == items.length) {
			if (head >= items.length / 2) {
				System.arraycopy(items, head, items, 0, tail - head);
				tail -= head;
				head = 0;
			} else {
				items = Arrays.copyOf(items, items.length * 2);
			}
		}
		items[tail++] = value;
	}

	boolean isEmpty() {
		return head == tail;
	}

	int remove() {
		return items[head++];
	}
}
