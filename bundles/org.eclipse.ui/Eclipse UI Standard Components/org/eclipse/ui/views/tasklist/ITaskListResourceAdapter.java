package org.eclipse.ui.views.tasklist;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;

/**
 * <code>ITaskListResourceAdapter</code> is an adapter interface that
 * supplies the resource to query for markers to display in the task list.
 * 
 * Implementors of this interface are typically registered with an
 * IAdapterFactory for lookup via the getAdapter() mechanism.
 */
public interface ITaskListResourceAdapter {

	/**
	 * Returns the resource to query for the markers to display
	 * for the given adaptable.
	 * 
	 * @param adaptable the adaptable being queried.
	 * @return the resource or <code>null</code> if there
	 * 	is no adapted resource for this object.
	 */
	public IResource getAffectedResource(IAdaptable adaptable);



}