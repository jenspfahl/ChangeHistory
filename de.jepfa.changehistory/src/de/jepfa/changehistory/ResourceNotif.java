package de.jepfa.changehistory;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.resources.IResource;
import org.eclipse.ui.ISharedImages;


/**
 * @author Jens Pfahl
 */
public class ResourceNotif {
	
	private static AtomicInteger totalCounter = new AtomicInteger();
	
	public enum State {
		
		ADDED("was added", ISharedImages.IMG_OBJ_ADD), 
		REMOVED("was removed", ISharedImages.IMG_ETOOL_DELETE), 
		CHANGED("has changed", ISharedImages.IMG_ETOOL_SAVE_EDIT), 
		REPLACED("has replaced", ISharedImages.IMG_ELCL_SYNCED);
		
		private String text;
		private String imageId; 
		private State(String text, String imageId) {
			this.text = text;
			this.imageId = imageId;
		}
		
		public String getText() {
			return text;
		}
		
		public String getImageId() {
			return imageId;
		}
		
		@Override
		public String toString() {
			return super.toString()+" means "+getText() + " with image " + getImageId();
		}
	};
	
	private IResource resource;
	private Date changed;
	private State state;
	private int id;
	public ResourceNotif(IResource resource, Date changed, State state) {
		synchronized (totalCounter) {
			this.resource = resource;
			this.changed = changed;
			this.state = state;
			id = totalCounter.getAndIncrement();
		}
	}
	
	public IResource getResource() {
		return resource;
	}
	
	public Date getChanged() {
		return changed;
	}
	
	public State getState() {
		return state;
	}
	
	public int getAge() {
		return totalCounter.get() - id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((resource == null) ? 0 : resource.getFullPath().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ResourceNotif other = (ResourceNotif) obj;
		if (resource == null) {
			if (other.resource != null)
				return false;
		}
		else if (!resource.getFullPath().equals(other.resource.getFullPath()))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ResourceNotif [resource=" + resource.getFullPath() + ", changed=" + changed + ", state=" + state + "]";
	}
	
	

}
