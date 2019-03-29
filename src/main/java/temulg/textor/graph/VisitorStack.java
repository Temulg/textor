/*
 * Copyright (c) 2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.textor.graph;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;

class VisitorStack<T> {
	static class Frame<T> {
		Frame(Vertex<T> v_, Iterator<Vertex<T>> iter_) {
			v = v_;
			iter = iter_;
		}

		final Vertex<T> v;
		final Iterator<Vertex<T>> iter;
	}

	void clear() {
		visited.clear();
		pos = 0;
		Arrays.fill(frames, null);
	}

	Frame<T> head() {
		return frames[pos - 1];
	}

	void push(Vertex<T> v, Iterator<Vertex<T>> iter) {
		if (pos == frames.length)
			frames = Arrays.copyOf(frames, (frames.length * 3) / 2);

		frames[pos++] = new Frame<>(v, iter);
	}

	boolean pop() {
		pos--;
		frames[pos] = null;
		return pos != 0;
	}

	boolean checkVisited(Vertex<T> v) {
		return null != visited.put(v, Boolean.TRUE);
	}

	final IdentityHashMap<
		Vertex<T>, Boolean
	> visited = new IdentityHashMap<>();

	@SuppressWarnings("unchecked")
	private Frame<T>[] frames = (Frame<T>[])new Frame[8];
	private int pos = 0;
}
