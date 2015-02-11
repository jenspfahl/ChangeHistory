package de.jepfa.changehistory.startup;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import de.jepfa.changehistory.MyResourceChangeReporter;
import de.jepfa.changehistory.views.ChangeHistoryView;


/**
 * Startup collecting changes.
 * 
 * @author Jens Pfahl
 */
public class PluginStartUp implements IStartup {

	@Override
	public void earlyStartup() {
		
		IResourceChangeListener listener = new MyResourceChangeReporter();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				listener, IResourceChangeEvent.POST_CHANGE);
		
		// activate view when visible
		final IWorkbench workbench = PlatformUI.getWorkbench();
		 workbench.getDisplay().asyncExec(new Runnable() {
		   @Override
		public void run() {
		     IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		     if (window != null && window.getActivePage() != null) {
		    	 IViewPart view = window.getActivePage().findView(ChangeHistoryView.ID);
		    	 if (view != null && window.getActivePage().isPartVisible(view)) {
		    		 view.setFocus();
		    	 }
		     }
		   }
		 });

		
		//Platform.getLog(Activator.getDefault().getBundle()).log(new Status(IStatus.INFO, Activator.PLUGIN_ID, "HERE I AM!"));
	}
}
