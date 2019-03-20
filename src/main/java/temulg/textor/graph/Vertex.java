/*
 * Copyright (c) 2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.textor.graph;

import java.util.HashMap;
import java.util.concurrent.locks.StampedLock;

public final class Vertex<T> {
	Vertex(T label_) {
		label = label_;
	}

	boolean addEdge(Vertex<T> other) {
		long stamp = nextLock.writeLock();
		long otherStamp = other.prevLock.writeLock();
		try {
			switch (addNext(other) + other.addPrev(this)) {
			case 0:
				return false;
			case 2:
				return true;
			default:
				throw new IllegalStateException(
					"Inconsistent graph state"
				);
			}
		} finally {
			other.prevLock.unlockWrite(otherStamp);
			nextLock.unlockWrite(stamp);
		}
	}

	boolean removeEdge(Vertex<T> other) {
		long stamp = nextLock.writeLock();
		long otherStamp = other.prevLock.writeLock();

		try {
			switch (removeNext(other) + other.removePrev(this)) {
			case 0:
				return false;
			case 2:
				return true;
			default:
				throw new IllegalStateException(
					"Inconsistent graph state"
				);
			}
		} finally {
			other.prevLock.unlockWrite(otherStamp);
			nextLock.unlockWrite(stamp);
		}
	}

	private int addNext(Vertex<T> other) {
		while (true) {
			int rv = next.add(other);
			if (rv >= 0)
				return rv;
			else
				next = next.grow();
		}
	}

	private int addPrev(Vertex<T> other) {
		while (true) {
			int rv = prev.add(other);
			if (rv >= 0)
				return rv;
			else
				prev = prev.grow();
		}
	}

	private int removeNext(Vertex<T> other) {
		int rv = next.remove(other);
		if (rv < 2)
			return rv;
		else {
			next = next.shrink();
			return rv - 1;
		}
	}

	private int removePrev(Vertex<T> other) {
		int rv = prev.remove(other);
		if (rv < 2)
			return rv;
		else {
			prev = prev.shrink();
			return rv - 1;
		}
	}

	private static interface EdgeSet<T> {
		int add(Vertex<T> v);

		int remove(Vertex<T> v);

		EdgeSet<T> grow();

		EdgeSet<T> shrink();
	}

	private static class EmptyEdgeSet<T> implements EdgeSet<T> {
		@Override
		public int add(Vertex<T> v) {
			return -1;
		}

		@Override
		public int remove(Vertex<T> v) {
			return 0;
		}

		@Override
		public EdgeSet<T> grow() {
			return new SingleEdgeSet<>();
		}

		@Override
		public EdgeSet<T> shrink() {
			return this;
		}
	}

	private static class SingleEdgeSet<T> implements EdgeSet<T> {
		@Override
		public int add(Vertex<T> v) {
			if (vs != null)
				return v.equals(vs) ? 0 : -1;
			else {
				vs = v;
				return 1;
			}
		}

		@Override
		public int remove(Vertex<T> v) {
			return v.equals(vs) ? 2 : 0;
		}

		@Override
		public EdgeSet<T> grow() {
			var ns = new ArrayEdgeSet<T>();
			ns.add(vs);
			return ns;
		}

		@Override
		public EdgeSet<T> shrink() {
			return new EmptyEdgeSet<>();
		}

		private Vertex<T> vs;
	}

	private static class ArrayEdgeSet<T> implements EdgeSet<T> {
		@Override
		public int add(Vertex<T> v) {
			for (int pos = 0; pos < vs.length; pos++) {
				if (vs[pos] != null) {
					if (v.equals(vs[pos]))
						return 0;
				} else {
					vs[pos] = v;
					return 1;
				}
			}
			return -1;
		}

		@Override
		public int remove(Vertex<T> v) {
			int pos = 0;
			for (; pos < vs.length; pos++) {
				if (vs[pos] != null) {
					if (v.equals(vs[pos])) {
						vs[pos] = null;
						for (pos++; pos < vs.length; pos++) {
							if (vs[pos] != null) {
								vs[pos - 1] = vs[pos];
								vs[pos] = null;
							} else
								break;
						}
						return vs[1] != null ? 1 : 2;
					}
				} else
					break;
			}
			return 0;
		}

		@Override
		public EdgeSet<T> grow() {
			var ns = new HashEdgeSet<T>();
			for (var v: vs)
				ns.add(v);

			return ns;
		}

		@Override
		public EdgeSet<T> shrink() {
			var ns = new SingleEdgeSet<T>();
			ns.vs = vs[0];
			return ns;
		}

		@SuppressWarnings("unchecked")
		private final Vertex<T>[] vs = (Vertex<T>[])new Vertex[
			ARRAY_SET_SIZE
		];
	}

	private static class HashEdgeSet<T> implements EdgeSet<T> {
		@Override
		public int add(Vertex<T> v) {
			return vs.put(v, Boolean.TRUE) != null ? 0 : 1;
		}

		@Override
		public int remove(Vertex<T> v) {
			if (vs.remove(v) != null) {
				return  vs.size() > ARRAY_SET_SIZE ? 1 : 2;
			} else
				return 0;
		}

		@Override
		public EdgeSet<T> grow() {
			return this;
		}

		@Override
		public EdgeSet<T> shrink() {
			var ns = new ArrayEdgeSet<T>();
			int pos = 0;
			for (var k: vs.keySet()) {
				ns.vs[pos] = k;
				pos++;
			}
			return ns;
		}

		private final HashMap<Vertex<T>, Boolean> vs = new HashMap<>();
	}

	private static final int ARRAY_SET_SIZE = 8;
	public final T label;
	private final StampedLock nextLock = new StampedLock();
	private final StampedLock prevLock = new StampedLock();
	private volatile EdgeSet<T> next = new EmptyEdgeSet<>();
	private volatile EdgeSet<T> prev = new EmptyEdgeSet<>();
}
