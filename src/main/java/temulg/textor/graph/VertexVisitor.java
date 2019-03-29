/*
 * Copyright (c) 2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.textor.graph;

@FunctionalInterface
public interface VertexVisitor<T> {
	void visitVertex(Vertex<T> v);

	default void endVisit() {
	}
}
