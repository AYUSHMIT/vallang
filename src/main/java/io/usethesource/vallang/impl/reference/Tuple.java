/*******************************************************************************
 * Copyright (c) 2007-2013 IBM Corporation & CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *******************************************************************************/
package io.usethesource.vallang.impl.reference;

import java.util.Iterator;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;

/*package*/ class Tuple  implements ITuple {

	protected final Type fType;
	protected final IValue[] fElements;

	/*package*/ Tuple(IValue... elements) {
		super();
		this.fType = TypeFactory.getInstance().tupleType(elements);
		this.fElements = elements;
	}

	private Tuple(Tuple other, int i, IValue elem) {
		this.fType = other.getType();
		fElements = other.fElements.clone();
		fElements[i] = elem;
	}

	@Override
	public Type getType() {
		return fType;
	}

	@Override
	public Iterator<IValue> iterator() {
		return new Iterator<IValue>() {
			private int count = 0;

			@Override
			public boolean hasNext() {
				return count < arity();
			}

			@Override
			public IValue next() {
				return get(count++);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("Can not remove elements from a tuple");
			}
		};
	}

	@Override
	public int hashCode() {
		int hash = 0;

		for (int i = 0; i < fElements.length; i++) {
			hash = (hash << 1) ^ (hash >> 1) ^ fElements[i].hashCode();
		}
		return hash;
	}

	@Override
	public String toString() {
	    return defaultToString();
	}
	
	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		} else if (o == null) {
			return false;
		} else if (getClass() == o.getClass()) {
			Tuple peer = (Tuple) o;

			if (!fType.comparable(peer.fType)) {
				return false;
			}

			int arity = arity();
			if (arity != peer.arity()) {
				return false;
			}
			for (int i = 0; i < arity; i++) {
				if (!get(i).equals(peer.get(i))) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public IValue get(int i) throws IndexOutOfBoundsException {
		try {
			return fElements[i];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IndexOutOfBoundsException("Tuple index " + i + " is larger than tuple width " + arity());
		}
	}

	@Override
	public IValue get(String label) throws FactTypeUseException {
		return fElements[fType.getFieldIndex(label)];
	}

	@Override
	public ITuple set(int i, IValue arg) throws IndexOutOfBoundsException {
		return new Tuple(this, i, arg);
	}

	@Override
	public ITuple set(String label, IValue arg) throws FactTypeUseException {
		int i = fType.getFieldIndex(label);
		return new Tuple(this, i, arg);
	}

	@Override
	public int arity() {
		return fElements.length;
	}

	@Override
	public IValue select(int... fields) throws IndexOutOfBoundsException {
		Type type = fType.select(fields);

		if (type.isFixedWidth()) {
			return doSelect(type, fields);
		}

		return get(fields[0]);
	}

	@Override
	public IValue selectByFieldNames(String... fields) throws FactTypeUseException {
		Type type = fType.select(fields);

		if (type.isFixedWidth()) {
			int[] indexes = new int[fields.length];
			int i = 0;
			for (String name : fields) {
				indexes[i] = type.getFieldIndex(name);
			}

			return doSelect(type, indexes);
		}

		return get(fields[0]);
	}

	private IValue doSelect(Type type, int... fields) throws IndexOutOfBoundsException {
		if (fields.length == 1)
			return get(fields[0]);
		IValue[] elems = new IValue[fields.length];
		Type[] elemTypes = new Type[fields.length];
		for (int i = 0; i < fields.length; i++) {
			elems[i] = get(fields[i]);
			elemTypes[i] = elems[i].getType();
		}
		return new Tuple(elems);
	}
}
