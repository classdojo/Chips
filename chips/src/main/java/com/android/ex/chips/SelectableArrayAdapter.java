package com.android.ex.chips;

import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Created by Roman on 4/12/2015.
 */
public abstract class SelectableArrayAdapter<T extends IRecipientEntry> extends BaseAdapter implements Filterable {
	protected List<T> mData;
	protected List<T> mFilteredData;
	private boolean[] mSelectedPositions;
	private boolean[] mFilteredSelectedPositions;
	private boolean mEditMode;
	private SelectDataChangeListener mListener;
	private Filter mFilter = new Filter() {
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();
			String filter = constraint != null ? constraint.toString() : null;
			List<T> list = getObjects();
			if(filter == null || filter.isEmpty()) {
				return results;
			}

			List<T> filteredList = new ArrayList<>();
			Iterator<T> itChannels = list.iterator();
			T channel;
			while(itChannels.hasNext()) {
				channel = itChannels.next();
				if(channel.getDisplayName().toLowerCase().contains(filter.toLowerCase()) || (channel.getDestination() != null && channel.getDestination().toLowerCase().contains(filter.toLowerCase()))) {
					filteredList.add(channel);
				}
			}
			results.count = filteredList.size();
			results.values = filteredList;
			return results;
		}


		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			setFilteredObjects((List<T>) results.values);
		}
	};


	public SelectableArrayAdapter() {
		mData = new ArrayList<>(0);
		mEditMode = false;
		mSelectedPositions = new boolean[0];
	}


	public SelectableArrayAdapter(List<T> data) {
		mData = data;
		mFilteredData = null;
		mEditMode = false;
		mSelectedPositions = new boolean[mData.size()];
		mFilteredSelectedPositions = null;
	}


	@Override
	public int getCount() {
		if(mFilteredData != null) {
			return mFilteredData.size();
		} else {
			return mData.size();
		}
	}


	@Override
	public T getItem(int i) {
		if(mFilteredData != null) {
			return mFilteredData.get(i);
		} else {
			return mData.get(i);
		}

	}


	@Override
	public long getItemId(int i) {
		return getObjects().get(i).getDataId();
	}


	public void reloadObjects(List<T> objects) {
		mData = objects;
		notifyDataSetChanged();
	}


	public boolean isEditMode() {
		return mEditMode;
	}


	public void setEditMode(boolean b) {
		mEditMode = b;
		notifyDataSetChanged();
	}


	public boolean isSelected(int position) {
		if(mFilteredSelectedPositions != null) {
			return mFilteredSelectedPositions[position];
		} else {
			return mSelectedPositions[position];
		}
	}


	public void setSelected(int position, boolean isSelected) {
		if(mFilteredSelectedPositions != null && position < mFilteredSelectedPositions.length) {
			mFilteredSelectedPositions[position] = isSelected;
		} else {
			mSelectedPositions[position] = isSelected;
		}
		notifyDataSetChanged();
	}


	public void toggleSelected(int position) {
		if(mFilteredSelectedPositions != null) {
			setSelected(position, !mFilteredSelectedPositions[position]);
		} else {
			setSelected(position, !mSelectedPositions[position]);
		}
	}


	public void selectAll() {
		if(mFilteredSelectedPositions != null) {
			for(int i = mFilteredSelectedPositions.length - 1; i >= 0; i--) {
				mFilteredSelectedPositions[i] = true;
			}
		} else {
			for(int i = mSelectedPositions.length - 1; i >= 0; i--) {
				mSelectedPositions[i] = true;
			}
		}
		notifyDataSetChanged();
	}


	public void deselectAll() {
		if(mFilteredSelectedPositions != null) {
			for(int i = mFilteredSelectedPositions.length - 1; i >= 0; i--) {
				mFilteredSelectedPositions[i] = false;
			}
		} else {
			for(int i = mSelectedPositions.length - 1; i >= 0; i--) {
				mSelectedPositions[i] = false;
			}
		}

		notifyDataSetChanged();
	}


	public boolean areAllSelected() {
		if(mFilteredSelectedPositions != null) {
			for(int i = mFilteredSelectedPositions.length - 1; i >= 0; i--) {
				if(!mFilteredSelectedPositions[i]) {
					return false;
				}
			}
		} else {
			for(int i = mSelectedPositions.length - 1; i >= 0; i--) {
				if(!mSelectedPositions[i]) {
					return false;
				}
			}
		}

		return getCount() != 0;
	}


	public boolean areSomeSelected() {
		for(int i = mSelectedPositions.length - 1; i >= 0; i--) {
			if(mSelectedPositions[i]) {
				return true;
			}
		}
		return false;
	}


	public int countSelected() {
		int count = 0;
		if(mFilteredSelectedPositions != null) {
			for(int i = mFilteredSelectedPositions.length - 1; i >= 0; i--) {
				if(mFilteredSelectedPositions[i]) {
					count += 1;
				}
			}
		} else {
			for(int i = mSelectedPositions.length - 1; i >= 0; i--) {
				if(mSelectedPositions[i]) {
					count += 1;
				}
			}
		}

		return count;
	}


	/**
	 * Toggles between item selection.
	 *
	 * @return True if all items were selected, false otherwise.
	 */
	public boolean toggleSelectAll() {
		boolean deselectAll = areAllSelected();
		if(deselectAll) {
			deselectAll();
		} else {
			selectAll();
		}
		if(mListener != null) {
			mListener.selectAll(!deselectAll);
		}
		return !deselectAll;
	}


	public List<T> getSelectedObjects() {
		List<T> selected = new ArrayList<>();
		if(mFilteredData != null) {
			for(int i = 0, max = mFilteredSelectedPositions.length; i < max; i++) {
				if(mFilteredSelectedPositions[i]) {
					selected.add(mFilteredData.get(i));
				}
			}
		} else {
			selected = getAllSelectedObjects();
		}


		return selected;
	}


	public List<T> getObjects() {
		return mData;
	}


	public void setObjects(List<T> objects) {
		mData = objects;
		mFilteredData = null;
		mEditMode = false;
		mSelectedPositions = new boolean[mData.size()];
		mFilteredSelectedPositions = null;
		notifyDataSetChanged();
	}


	public void setFilteredObjects(List<T> objects) {
		setFilteredSelectedPositions(objects);
		mFilteredData = objects;
		if(mListener != null) {
			mListener.dataChanged();
		}
		notifyDataSetChanged();
	}


	@Override
	public Filter getFilter() {
		return mFilter;
	}


	public void registerSelectedDataChangeListener(SelectDataChangeListener listener) {
		mListener = listener;
	}


	public List<T> getAllSelectedObjects() {
		List<T> selected = new ArrayList<>();
		for(int i = 0, max = Math.min(mSelectedPositions.length, mData.size()); i < max; i++) {
			if(mSelectedPositions[i]) {
				selected.add(mData.get(i));
			}
		}
		return selected;
	}


	private void setFilteredSelectedPositions(List<T> objects) {
		T item;
		Iterator<T> itItems;
		int position;

		if(objects == null) {
			if(mFilteredSelectedPositions != null) {
				for(int i = 0, max = mFilteredSelectedPositions.length; i < max; i++) {
					itItems = mData.iterator();
					position = 0;
					while(itItems.hasNext()) {
						item = itItems.next();
						if(item.equals(mFilteredData.get(i))) {
							mSelectedPositions[position] = mFilteredSelectedPositions[i];
						}
						position++;
					}

				}
			}

			mFilteredSelectedPositions = null;
			return;
		}

		if(mFilteredSelectedPositions != null && objects.size() <= mFilteredData.size()) {
			boolean positions[] = new boolean[objects.size()];
			for(int i = 0, max = mFilteredSelectedPositions.length; i < max; i++) {
				itItems = objects.iterator();
				position = 0;
				while(itItems.hasNext()) {
					item = itItems.next();
					if(item.equals(mFilteredData.get(i))) {
						positions[position] = mFilteredSelectedPositions[i];
					}
					position++;
				}
			}
			mFilteredSelectedPositions = positions;
		} else {
			mFilteredSelectedPositions = new boolean[objects.size()];
			for(int i = 0, max = mSelectedPositions.length; i < max; i++) {
				itItems = objects.iterator();
				position = 0;
				while(itItems.hasNext()) {
					item = itItems.next();
					if(item.equals(mData.get(i))) {
						mFilteredSelectedPositions[position] = mSelectedPositions[i];
					}
					position++;
				}

			}
		}

	}


	public interface SelectDataChangeListener {
		void selectAll(boolean selected);

		void dataChanged();
	}
}

