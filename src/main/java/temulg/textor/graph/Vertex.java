/*
 * Copyright (c) 2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.textor.graph;

public final class Vertex<T> {
	Vertex(T value_) {
		value = value_;
	}

	private final T value;
}
