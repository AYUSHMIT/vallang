/*******************************************************************************
 * Copyright (c) 2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *******************************************************************************/
package io.usethesource.vallang.impl;

import io.usethesource.vallang.IMap;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.visitors.IValueVisitor;

public abstract class AbstractMap extends AbstractValue implements IMap {

    public AbstractMap() {
    	super();
    }

    protected static TypeFactory getTypeFactory() {
        return TypeFactory.getInstance();
    }

    protected static Type inferMapType(final Type candidateMapType, final java.util.Map<IValue, IValue> content) {
    	return inferMapType(candidateMapType, content.isEmpty());
    }
    
    protected static Type inferMapType(final Type candidateMapType, final boolean isEmpty) {
		if (!candidateMapType.isMap())
			throw new IllegalArgumentException("Type must be a map type: "
					+ candidateMapType);
    	
    	final Type inferredCollectionType;
                       
        // is collection empty?
        if (isEmpty) {
			inferredCollectionType = getTypeFactory().mapType(
					getTypeFactory().voidType(), candidateMapType.getKeyLabel(),
					getTypeFactory().voidType(), candidateMapType.getValueLabel());

        } else {
        	inferredCollectionType = candidateMapType;
        }

        return inferredCollectionType;
    }

    protected abstract IValueFactory getValueFactory();

    @Override
    public <T, E extends Throwable> T accept(IValueVisitor<T, E> v) throws E {
        return v.visitMap(this);
    }

	@Override
	public Type getKeyType() {
		return getType().getKeyType();
	}

	@Override
	public Type getValueType() {
		return getType().getValueType();
	}
}
