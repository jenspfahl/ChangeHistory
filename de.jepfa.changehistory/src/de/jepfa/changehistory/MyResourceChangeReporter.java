package de.jepfa.changehistory;

import java.util.Date;
import java.util.regex.Pattern;

import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import de.jepfa.changehistory.ResourceNotif.State;
import de.jepfa.changehistory.internal.Activator;

/**
 * Creates/refreshes entries in the view table.
 */
class DeltaPrinter implements IResourceDeltaVisitor {
	
	private static final int MAX_SIZE = 1000;

	@Override
	public boolean visit(IResourceDelta delta) {
		if (!Activator.isEnabled()) {
			return false;
		}
		
		IResource res = delta.getResource();
		ResourceNotif resourceNotif = null;
		
		Pattern exludePattern = Activator.getExludeExpression();
		String fullPath = res.getFullPath().toString();
		if (exludePattern == null || !exludePattern.matcher(fullPath).find()) {
			switch (delta.getKind()) {
				case IResourceDelta.ADDED:
					resourceNotif = new ResourceNotif(res, new Date(), State.ADDED);
					break;
				case IResourceDelta.REMOVED:
					resourceNotif = new ResourceNotif(res, new Date(), State.REMOVED);
					break;
				case IResourceDelta.CHANGED:
					int flags = delta.getFlags();
					if ((flags & IResourceDelta.CONTENT) != 0) {
						resourceNotif = new ResourceNotif(res, new Date(), State.CHANGED);
					}
					if ((flags & IResourceDelta.REPLACED) != 0) {
						resourceNotif = new ResourceNotif(res, new Date(), State.REPLACED);
					}
	
					break;
			}
		}
		
		if (resourceNotif != null) {
			if (Activator.getHistory() != null) {
				final ResourceNotif fResourceNotif = resourceNotif; 
				Activator.getHistory().getRealm().asyncExec(new Runnable() {
					
					@Override
					public void run() {
						IObservableList history = Activator.getHistory();
						synchronized (history) {
							if (history.contains(fResourceNotif)) {
								history.remove(fResourceNotif);
							}
							history.add(fResourceNotif);
							if (history.size() >= MAX_SIZE) {
								history.remove(0);
							}
						}
					}
				});
			}
		}

		return true; // visit the children
	}
}

/**
 * Responsible for creating/refreshing entries in the view table.
 * 
 * @author Jens Pfahl
 */
public class MyResourceChangeReporter implements IResourceChangeListener {

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			switch (event.getType()) {
				case IResourceChangeEvent.POST_CHANGE:
					event.getDelta().accept(new DeltaPrinter());
					break;
				
			}
		}
		catch (CoreException e) {
			Platform.getLog(Activator.getDefault().getBundle()).log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
		}
	}
}
