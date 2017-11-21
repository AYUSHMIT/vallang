/*******************************************************************************
 * Copyright (c) 2015 CWI
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

import static io.usethesource.vallang.util.EqualityUtils.KEYWORD_PARAMETER_COMPARATOR;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.type.TypeStore;
import io.usethesource.vallang.IAnnotatable;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.IWithKeywordParameters;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.impl.func.ConstructorFunctions;
import io.usethesource.vallang.io.StandardTextWriter;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.visitors.IValueVisitor;

public class ConstructorWithKeywordParametersFacade implements IConstructor {
	protected final IConstructor content;
	protected final io.usethesource.capsule.Map.Immutable<String, IValue> parameters;
	
	public ConstructorWithKeywordParametersFacade(final IConstructor content, final io.usethesource.capsule.Map.Immutable<String, IValue> parameters) {
		this.content = content;
		this.parameters = parameters;
	}
	
	public Type getType() {
		return content.getType();
	}

	public <T, E extends Throwable> T accept(IValueVisitor<T, E> v) throws E {
		return v.visitConstructor(this);
	}

	public IValue get(int i) throws IndexOutOfBoundsException {
		return content.get(i);
	}
	
	public IConstructor set(int i, IValue newChild) throws IndexOutOfBoundsException {
		IConstructor newContent = content.set(i, newChild);
		return new ConstructorWithKeywordParametersFacade(newContent, parameters); // TODO: introduce wrap() here as well
	}

	public int arity() {
		return content.arity();
	}

	public String toString() {
		return StandardTextWriter.valueToString(this);
	}

	public String getName() {
		return content.getName();
	}

	public Iterable<IValue> getChildren() {
		return content.getChildren();
	}

	public Iterator<IValue> iterator() {
		return content.iterator();
	}
	
	public IConstructor replace(int first, int second, int end, IList repl)
			throws FactTypeUseException, IndexOutOfBoundsException {
	  throw new UnsupportedOperationException("Replace not supported on constructor.");
	}

	public boolean equals(Object o) {
		if(o == this) return true;
		if(o == null) return false;
		
		if(o.getClass() == getClass()){
			ConstructorWithKeywordParametersFacade other = (ConstructorWithKeywordParametersFacade) o;
		
			return content.equals(other.content) &&
					parameters.equals(other.parameters);
		}
		
		return false;
	}

	@Override
	public boolean isEqual(IValue other) {
	  if (!(other instanceof ConstructorWithKeywordParametersFacade)) {
		  if (other instanceof IConstructor) {
			  IConstructor oc = ((IConstructor)other);
			  if (content.isEqual(oc) && oc.mayHaveKeywordParameters()) {
				  IWithKeywordParameters<? extends IConstructor> ocw = oc.asWithKeywordParameters();
				  return ocw.getParameters().equals(parameters);
			  }
			  else {
				  return false;
			  }
		  }
		  else {
			  return false;
		  }
	  }

	  ConstructorWithKeywordParametersFacade o = (ConstructorWithKeywordParametersFacade) other;

	  return content.isEqual(o.content) && KEYWORD_PARAMETER_COMPARATOR.equals(parameters, o.parameters);
	}

	@Override
    public boolean match(IValue other) {
	    if (other instanceof ConstructorWithKeywordParametersFacade) {
	        return content.match(((ConstructorWithKeywordParametersFacade) other).content);    
	    }
	    
	    if (other instanceof IConstructor) {
	        return ConstructorFunctions.match(this, other);
	    }
	    
	    return false;
    }
	
	@Override
	public int hashCode() {
		return 131 + 3 * content.hashCode() + 101 * parameters.hashCode();
	}
	
	@Override
	public boolean isAnnotatable() {
		return false;
	}
	
	@Override
	public IAnnotatable<? extends IConstructor> asAnnotatable() {
		throw new UnsupportedOperationException("can not annotate a constructor which already has keyword parameters");
	}
	
	@Override
	public boolean mayHaveKeywordParameters() {
	  return true;
	}
	
	@Override
	public IWithKeywordParameters<? extends IConstructor> asWithKeywordParameters() {
	  return new AbstractDefaultWithKeywordParameters<IConstructor>(content, parameters) {
      @Override
      protected IConstructor wrap(IConstructor content, io.usethesource.capsule.Map.Immutable<String, IValue> parameters) {
        return new ConstructorWithKeywordParametersFacade(content, parameters);
      }
      
      @Override
      public boolean hasParameters() {
        return parameters != null && parameters.size() > 0;
      }
      
      @Override
      public Set<String> getParameterNames() {
        return parameters.keySet();		
      }
      
      @Override
      public Map<String, IValue> getParameters() {
        return Collections.unmodifiableMap(parameters);
      }
    };
	}

  @Override
  public Type getConstructorType() {
    return content.getConstructorType();
  }

  @Override
  public Type getUninstantiatedConstructorType() {
    return content.getUninstantiatedConstructorType();
  }

  @Override
  public IValue get(String label) {
    return content.get(label);
  }

  @Override
  public IConstructor set(String label, IValue newChild) throws FactTypeUseException {
    return new ConstructorWithKeywordParametersFacade(content.set(label, newChild), parameters);
  }

  @Override
  public boolean has(String label) {
    return content.has(label);
  }

  
  @Override
  public Type getChildrenTypes() {
    return content.getChildrenTypes();
  }

  @Override
  public boolean declaresAnnotation(TypeStore store, String label) {
    return content.declaresAnnotation(store, label);
  }
}
