package de.jepfa.changehistory.views;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.regex.Pattern;

import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.set.ISetChangeListener;
import org.eclipse.core.databinding.observable.set.SetChangeEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;

import de.jepfa.changehistory.ResourceNotif;
import de.jepfa.changehistory.internal.Activator;

/**
 * The Change History view.
 * 
 * @author Jens Pfahl
 */
public class ChangeHistoryView extends ViewPart {
	
	private ImageDescriptor filterImage = ImageDescriptor.createFromURL(
    		FileLocator.find(Activator.getDefault().getBundle(), new Path("icons/filter_history.gif"), null));


	private final class StartStopAction extends Action {

		private StartStopAction() {
			super("Start/Stop updating", SWT.TOGGLE);
			updateButtonState(true);
		}

		@Override
		public void run() {
			boolean newValue = !Activator.isEnabled();
			Activator.setEnabled(newValue);
			updateButtonState(newValue);
		}

		public void updateButtonState(boolean newValue) {
			setChecked(newValue);
			if (newValue) {
				 setToolTipText("Stop updating - now is ON");
			}
			else {
				 setToolTipText("Start updating - now is OFF");
			}
		}
	}

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "de.jepfa.changehistory.views.ChangeHistoryView";
	private static final DateFormat sdtf = SimpleDateFormat.getDateTimeInstance(
			SimpleDateFormat.MEDIUM, SimpleDateFormat.MEDIUM);
	private static final DateFormat stf = SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM);
	
	private static final ResourceManager manager = 
			new LocalResourceManager(JFaceResources.getResources());

	private TableViewer viewer;
	private StartStopAction startStopAction;
	private Action clearAction;
	private Action clearAllAction;
	private Action excludeFilterAction;
	private Action doubleClickAction;

	private IObservableList history;

	private Label headerLabel;



	public ChangeHistoryView() {

		history = new WritableList(new LinkedList<ResourceNotif>(), ResourceNotif.class);
		Activator.setHistory(history);
	}


	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(GridLayoutFactory.fillDefaults().create());
		headerLabel = new Label(parent, SWT.NONE);
		headerLabel.setLayoutData(GridDataFactory.fillDefaults().create());
		viewer = new TableViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
		ColumnViewerToolTipSupport.enableFor(viewer);
		viewer.getTable().setHeaderVisible(true);
		viewer.getTable().setLinesVisible(true);
		viewer.getTable().setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		
		TableViewerColumn colChangedAt = new TableViewerColumn(viewer, SWT.NONE);
		colChangedAt.getColumn().setWidth(70);
		colChangedAt.getColumn().setText("State");
		colChangedAt.setLabelProvider(new ColumnLabelProvider() {
			
			@Override
			public Image getImage(Object element) {
				ResourceNotif resourceNotif = (ResourceNotif) element;
				return PlatformUI.getWorkbench().getSharedImages().getImage(resourceNotif.getState().getImageId());
			};
			
			@Override
			public String getToolTipText(Object element) {
				ResourceNotif resourceNotif = (ResourceNotif) element;
				return resourceNotif.getState().getText() + " at " + sdtf.format(resourceNotif.getChanged());
			}

			@Override
			public String getText(Object element) {
				ResourceNotif resourceNotif = (ResourceNotif) element;
				
				return stf.format(resourceNotif.getChanged());
			}
			
			@Override
			public Color getForeground(Object element) {
				ResourceNotif resourceNotif = (ResourceNotif) element;
				return createColor(resourceNotif);
			}
			
		});
		TableViewerColumn colResourceProject = new TableViewerColumn(viewer, SWT.NONE);
		colResourceProject.getColumn().setWidth(250);
		colResourceProject.getColumn().setText("Project");
		colResourceProject.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				ResourceNotif resourceNotif = (ResourceNotif) element;
				return resourceNotif.getResource().getProject().getName();
			}
			
				
			@Override
			public Color getForeground(Object element) {
				ResourceNotif resourceNotif = (ResourceNotif) element;
				return createColor(resourceNotif);
			}
			
			@Override
			public String getToolTipText(Object element) {
				ResourceNotif resourceNotif = (ResourceNotif) element;
				return getRankString(resourceNotif);
			}
		});
		TableViewerColumn colResourcePath = new TableViewerColumn(viewer, SWT.NONE);
		colResourcePath.getColumn().setWidth(300);
		colResourcePath.getColumn().setText("Path");
		colResourcePath.setLabelProvider(new ColumnLabelProvider() {
			
			@Override
			public String getText(Object element) {
				ResourceNotif resourceNotif = (ResourceNotif) element;
				String name = resourceNotif.getResource().getName();
				String path = resourceNotif.getResource().getProjectRelativePath().toString();
				if (path.length() <= name.length()) {
					return "";
				}
				return path.substring(0, path.length() - name.length());
			}
			
			@Override
			public Color getForeground(Object element) {
				ResourceNotif resourceNotif = (ResourceNotif) element;
				return createColor(resourceNotif);
			}
			
			@Override
			public String getToolTipText(Object element) {
				ResourceNotif resourceNotif = (ResourceNotif) element;
				return getRankString(resourceNotif);
			}
		});
		TableViewerColumn colResourceName = new TableViewerColumn(viewer, SWT.NONE);
		colResourceName.getColumn().setWidth(300);
		colResourceName.getColumn().setText("Resource");
		colResourceName.setLabelProvider(new ColumnLabelProvider() {
			
			@Override
			public String getText(Object element) {
				ResourceNotif resourceNotif = (ResourceNotif) element;
				return resourceNotif.getResource().getName();
			}
			
			@Override
			public Color getForeground(Object element) {
				ResourceNotif resourceNotif = (ResourceNotif) element;
				return createColor(resourceNotif);
			}
			
			@Override
			public String getToolTipText(Object element) {
				ResourceNotif resourceNotif = (ResourceNotif) element;
				return getRankString(resourceNotif);
			}
		});

		ObservableListContentProvider contentProvider = new ObservableListContentProvider();
		contentProvider.getRealizedElements().addSetChangeListener(new ISetChangeListener() {
			
			@Override
			public void handleSetChange(SetChangeEvent event) {
				updateHeader();
				viewer.refresh();
				
			}
		});
		viewer.setContentProvider(contentProvider);
		viewer.setSorter(new ViewerSorter() {

			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				Date date1 = new Date(Long.MIN_VALUE);
				int age1 = 0;
				int age2 = 0;
				if (e1 != null) {
					ResourceNotif resourceNotif1 = (ResourceNotif) e1;
					age1 = resourceNotif1.getAge();
					date1 = resourceNotif1.getChanged();
				}
				Date date2 = new Date(Long.MAX_VALUE);
				if (e2 != null) {
					ResourceNotif resourceNotif2 = (ResourceNotif) e2;
					age2= resourceNotif2.getAge();
					date2 = resourceNotif2.getChanged();
				}
				if (date1.equals(date2)) {
					return Integer.valueOf(age1).compareTo(Integer.valueOf(age2));
				}
				return date2.compareTo(date1);
			}
		});
		viewer.addFilter(new ViewerFilter() {

			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element != null) {
					ResourceNotif resourceNotif = (ResourceNotif) element;
					Pattern exludePattern = Activator.getExludeExpression();
					String fullPath = resourceNotif.getResource().getFullPath().toString();
					if (exludePattern != null && exludePattern.matcher(fullPath).find()) {
						return false;
					}
				}
				return true;
			}
		});
		viewer.setInput(history);

		makeActions();
		hookDoubleClickAction();
		contributeToActionBars();
	}



	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
//		manager.add(clearAction);
//		manager.add(clearAllAction);
//		manager.add(new Separator());
//		manager.add(excludeFilterAction);
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(startStopAction);
		manager.add(clearAction);
		manager.add(clearAllAction);
		manager.add(excludeFilterAction);
	}

	private void makeActions() {
		startStopAction = new StartStopAction();
		startStopAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED));
		
		clearAction = new Action() {

			@Override
			public void run() {
				StructuredSelection selection = (StructuredSelection) viewer.getSelection();
				Activator.getHistory().removeAll(selection.toList());
				updateHeader();
				viewer.refresh();
			}
		};
		clearAction.setText("Clear selected items");
		clearAction.setToolTipText("Delete selected items");
		clearAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE));
		
		clearAllAction = new Action() {

			@Override
			public void run() {
				if (Activator.getHistory().isEmpty()) {
					return;
				}
				MessageDialog messageDialog = new MessageDialog(getSite().getShell(), "Delete all items", null,
						"Really delete all items?", MessageDialog.QUESTION, new String[] {"Delete", "Abort"}, 0);
				int rc = messageDialog.open();
				if (rc == Window.OK) {
					Activator.getHistory().clear();
					updateHeader();
				}
			}
		};
		clearAllAction.setText("Clear items");
		clearAllAction.setToolTipText("Delete all items");
		clearAllAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_ELCL_REMOVEALL));

		excludeFilterAction = new Action() {

			@Override
			public void run() {
				InputDialog exludeExpressionDialog = new InputDialog(getSite().getShell(), "Exlude items",
						"Input a regular expression to exlude items from this view",
						Activator.getExludeExpression() != null ? Activator.getExludeExpression().pattern() : null,
						null);
				int rc = exludeExpressionDialog.open();
				if (rc == Window.OK) {
					try {
						String value = exludeExpressionDialog.getValue();
						Pattern excludePattern = null;
						if (value != null && !value.trim().isEmpty()) {
							excludePattern = Pattern.compile(value);
						}
						Activator.setExludeExpression(excludePattern);
					}
					catch (Exception e) {
						showMessage("Your regex pattern is invalid: " + e.getMessage());
					}

					viewer.refresh();
					updateHeader();
				}
			}
		};
		excludeFilterAction.setText("Exlude items");
		excludeFilterAction.setToolTipText("Exlude items with a regular expression");
		excludeFilterAction.setImageDescriptor(filterImage);
		doubleClickAction = new Action() {

			@Override
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection) selection).getFirstElement();
				if (obj instanceof ResourceNotif) {
					ResourceNotif resourceNotif = (ResourceNotif) obj;
					IResource resource = resourceNotif.getResource();
					
					if (resource instanceof IFile) {
						IFile file = (IFile) resource;
						if (file.exists()) {
							try {
								IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), file);
							}
							catch (Exception e) {
								Platform.getLog(Activator.getDefault().getBundle()).log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
							}
						}
						else {
							showMessage("File " + file.getFullPath() + " not found!");
						}
					}
				}
			}
		};
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	private void showMessage(String message) {
		MessageDialog.openInformation(viewer.getControl().getShell(), "Change History", message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	private void updateHeader() {
		if (headerLabel != null && !headerLabel.isDisposed()) {
			headerLabel.setText(MessageFormat.format(
					"Total count: {0}  Exclude Filter: {1}", viewer.getTable().getItemCount(), 
					Activator.getExludeExpression() != null ? "\""+Activator.getExludeExpression().pattern()+"\"" : "-none-"));
		}
	}

	private int getLevel(ResourceNotif resourceNotif) {
		int weight = 20;
		return Math.min(-weight + resourceNotif.getAge() * weight, 180);
	}

	private Color createColor(ResourceNotif resourceNotif) {
		int l = getLevel(resourceNotif);
		if (resourceNotif.getAge() <= 3) {
			return manager.createColor(new RGB(250-(resourceNotif.getAge()*resourceNotif.getAge()*25), l, 0));
		}
		else {
			return manager.createColor(new RGB(l, l, 0));
		}
	}


	private String getRankString(ResourceNotif resourceNotif) {
		return "Rank " + resourceNotif.getAge();
	}

}