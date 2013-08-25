/**
 * @license
 *
 * Copyright (c) 2013 Gioacchino Mazzurco <gio@eigenlab.org>.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.retroshare.android.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
* A weak ListSet. An element stored in the WeakListSet might be
* garbage collected, if there is no strong reference to this element.
*/
public class WeakListSet<T> implements Set<T>
{
	private final Map<Integer, T> repository = new WeakHashMap<Integer, T>();

	@Override public int size() { return repository.size(); }
	@Override public Object[] toArray() { return repository.values().toArray(); }
	@Override public <T1> T1[] toArray(T1[] t1s) { return repository.values().toArray(t1s); }
	@Override public Iterator iterator() { return repository.values().iterator(); }
	@Override public boolean contains(Object o) { return repository.containsValue(o); }
	@Override public boolean containsAll(Collection<?> objects) { return repository.values().containsAll(objects); }
	@Override public boolean isEmpty() { return repository.isEmpty(); }
	@Override public boolean add(T e)
	{
		boolean ret = !repository.containsValue(e);
		if(ret) repository.put(new Integer(repository.size()), e);
		return ret;
	}
	@Override public boolean addAll(Collection<? extends T> ts)
	{
		boolean ret = false;
		for ( T e : ts )
		{
			boolean missing = ! repository.containsValue(e);
			if(missing)
			{
				ret = true;
				repository.put(Integer.valueOf(repository.size()), e);
			}
		}
		return ret;
	}
	@Override public void clear() { repository.clear(); }
	@Override public boolean remove(Object o)
	{
		boolean ret = repository.containsValue(o);
		if(ret) repository.remove(o);
		return ret;
	}
	@Override public boolean removeAll(Collection<?> objects)
	{
		boolean ret = false;
		for ( Object e : objects )
		{
			boolean present = repository.containsValue(e);
			if(present)
			{
				ret = true;
				repository.remove(e);
			}
		}
		return ret;
	}
	@Override public boolean retainAll(Collection<?> objects) { return repository.values().retainAll(objects); }
}
