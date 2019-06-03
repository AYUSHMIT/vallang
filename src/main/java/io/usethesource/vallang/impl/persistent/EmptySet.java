/*******************************************************************************
 * Copyright (c) 2013-2014 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *******************************************************************************/
package io.usethesource.vallang.impl.persistent;

import static io.usethesource.vallang.impl.persistent.SetWriter.asInstanceOf;
import static io.usethesource.vallang.impl.persistent.SetWriter.isTupleOfArityTwo;
import static io.usethesource.vallang.impl.persistent.ValueCollectors.toSet;
import static io.usethesource.vallang.impl.persistent.ValueCollectors.toSetMultimap;

import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Stream;

import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISetRelation;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.impl.AbstractSet;
import io.usethesource.vallang.impl.DefaultRelationViewOnSet;
import io.usethesource.vallang.type.Type;

public final class EmptySet extends AbstractSet {

  public static final EmptySet EMPTY_SET = new EmptySet();

  private EmptySet() {}

  public static final ISet of() {
    return EMPTY_SET;
  }

  public static final ISet of(final IValue firstElement) {
    final Type firstElementType = firstElement.getType();

    if (isTupleOfArityTwo.test(firstElementType)) {
      return Stream.of(firstElement).map(asInstanceOf(ITuple.class))
          .collect(toSetMultimap(firstElementType.getOptionalFieldName(0), tuple -> tuple.get(0),
              firstElementType.getOptionalFieldName(1), tuple -> tuple.get(1)));
    } else {
      return Stream.of(firstElement).collect(toSet());
    }
  }

  @Override
  public ITuple tuple(IValue... elems) {
      return ValueFactory.getInstance().tuple(elems);
  }
  
  @Override
  public ISetRelation<ISet> asRelation() {
    validateIsRelation(this);
    return new DefaultRelationViewOnSet(getValueFactory(), this);
  }
  
  @Override
  public ISetWriter writer() {
      return ValueFactory.getInstance().setWriter();
  }

  @Override
  protected IValueFactory getValueFactory() {
    return ValueFactory.getInstance();
  }

  @Override
  public Type getType() {
    return getTypeFactory().setType(getTypeFactory().voidType());
  }

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Override
  public ISet insert(IValue value) {
    // TODO: move smart constructor
    return of(value);
  }

  @Override
  public ISet delete(IValue value) {
    return this;
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public boolean contains(IValue value) {
    return false;
  }

  @Override
  public Iterator<IValue> iterator() {
    return Collections.emptyIterator();
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public boolean equals(Object other) {
    return other == this;
  }

  @Override
  public boolean isEqual(IValue other) {
    return other == this;
  }
  
  @Override
  public boolean match(IValue other) {
    return other == this;
  }

  @Override
  public ISet union(ISet other) {
    return other;
  }

  @Override
  public ISet intersect(ISet other) {
    return this;
  }

  @Override
  public ISet subtract(ISet other) {
    return this;
  }

  @Override
  public ISet product(ISet that) {
    return this;
  }

  @Override
  public boolean isSubsetOf(ISet other) {
    return true;
  }

}
