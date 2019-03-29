/*
 * Copyright (c) 2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.textor.graph;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class Digraph<T> implements Cloneable {
	public Digraph() {}

	public boolean addEdge(Vertex<T> src, Vertex<T> dst) {
		ownsVertex(src);
		ownsVertex(dst);
		return src.addEdge(dst);
	}

	public boolean containsEdge(Vertex<T> src, Vertex<T> dst) {
		ownsVertex(src);
		ownsVertex(dst);
		return src.containsEdge(dst);
	}

	public boolean removeEdge(Vertex<T> src, Vertex<T> dst) {
		ownsVertex(src);
		ownsVertex(dst);
		return src.removeEdge(dst);
	}

	public Vertex<T> makeVertex(T label) {
		return vertices.computeIfAbsent(label, Vertex<T>::new);
	}

	public void visitDepthFirst(
		VertexVisitor<T> visitor,
		Order order,
		Iterable<Vertex<T>> roots
	) {
		final var vs = new VisitorStack<T>();

		for (var v: roots)
			order.apply(v, vs, visitor);

		visitor.endVisit();
	}

	private static class VisitorFrame<T> {
		Vertex<T> v;
		Iterator<Vertex<T>> iter;
	}

	private interface VisitationOrder {
		<T> void apply(
			Vertex<T> v,
			VisitorStack<T> vs,
			VertexVisitor<T> visitor
		);
	}

	public enum Order implements VisitationOrder {
		PREORDER_FWD {
			@Override
			public <T> void apply(
				Vertex<T> v,
				VisitorStack<T> vs,
				VertexVisitor<T> visitor
			) {
				if (vs.checkVisited(v))
					return;

				vs.push(v, v.nextIter());
				visitor.visitVertex(vs.head().v);

				while (true) {
					var h = vs.head();
					if (h.iter.hasNext()) {
						var nv = h.iter.next();
						if (!vs.checkVisited(nv)) {
							visitor.visitVertex(nv);
							vs.push(nv, nv.nextIter());
						}
					} else if (!vs.pop())
						break;
				}
			}
		},
		PREORDER_REV {
			@Override
			public <T> void apply(
				Vertex<T> v,
				VisitorStack<T> vs,
				VertexVisitor<T> visitor
			) {
				if (vs.checkVisited(v))
					return;

				vs.push(v, v.prevIter());
				visitor.visitVertex(vs.head().v);

				while (true) {
					var h = vs.head();
					if (h.iter.hasNext()) {
						var nv = h.iter.next();
						if (!vs.checkVisited(nv)) {
							visitor.visitVertex(nv);
							vs.push(nv, nv.prevIter());
						}
					} else if (!vs.pop())
						break;
				}
			}
		},
		POSTORDER_FWD {
			@Override
			public <T> void apply(
				Vertex<T> v,
				VisitorStack<T> vs,
				VertexVisitor<T> visitor
			) {
				if (vs.checkVisited(v))
					return;

				vs.push(v, v.nextIter());

				while (true) {
					var h = vs.head();
					if (h.iter.hasNext()) {
						var nv = h.iter.next();
						if (!vs.checkVisited(nv)) {
							vs.push(nv, nv.nextIter());
						}
					} else {
						visitor.visitVertex(h.v);
						if (!vs.pop())
							break;
					}
				}
			}
		},
		POSTORDER_REV {
			@Override
			public <T> void apply(
				Vertex<T> v,
				VisitorStack<T> vs,
				VertexVisitor<T> visitor
			) {
				if (vs.checkVisited(v))
					return;

				vs.push(v, v.prevIter());

				while (true) {
					var h = vs.head();
					if (h.iter.hasNext()) {
						var nv = h.iter.next();
						if (!vs.checkVisited(nv)) {
							vs.push(nv, nv.prevIter());
						}
					} else {
						visitor.visitVertex(h.v);
						if (!vs.pop())
							break;
					}
				}
			}
		}
	}

	private final void ownsVertex(Vertex<T> v) {
		if (vertices.get(v.label) != v)
			throw new IllegalArgumentException(
				"vertex " + v + " is not owned by this graph"
			);
	}

	private final ConcurrentHashMap<
		T, Vertex<T>
	> vertices = new ConcurrentHashMap<>();
}
