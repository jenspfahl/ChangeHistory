package de.jepfa.changehistory.internal;

import java.util.regex.Pattern;

import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * @author Jens Pfahl
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "de.jepfa.changehistory"; //$NON-NLS-1$

	private static final String EXCLUDE_PATTERN_KEY = PLUGIN_ID+".excludepattern";

	// The shared instance
	private static Activator plugin;

	private static IObservableList history;
	
	private static Pattern exludeExpression;

	private static boolean enabled = true;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		loadExcludePattern();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		persistExcludePattern();
		
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}


	public static void setHistory(IObservableList value) {
		history = value;
	}
	
	
	/**
	 * @return list
	 */
	public static IObservableList getHistory() {
		return history;
	}
	

	

	public static Pattern getExludeExpression() {
		return exludeExpression;
	}


	public static void setExludeExpression(Pattern value) {
		exludeExpression = value;
		
	}
	
	public static void loadExcludePattern() {
		String exludePattern = getDefault().getDialogSettings().get(EXCLUDE_PATTERN_KEY);
		if (exludePattern != null && !exludePattern.isEmpty()) {
			exludeExpression = Pattern.compile(exludePattern);
		}
	}
	
	public static void persistExcludePattern() {
		Activator.getDefault().getDialogSettings().put(EXCLUDE_PATTERN_KEY, 
				exludeExpression != null ? exludeExpression.pattern(): null);
	}


	public static boolean isEnabled() {
		return enabled;
	}


	public static void setEnabled(boolean newValue) {
		enabled = newValue;
	}


}
