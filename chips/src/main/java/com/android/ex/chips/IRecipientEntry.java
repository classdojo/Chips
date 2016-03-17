package com.android.ex.chips;

/**
 * Created by Roman on 4/12/2015.
 */
public interface IRecipientEntry {
	String getDisplayName();

	String getDestination();

	boolean drawPhotos();

	byte[] getPhotoBytes();

	void getPhotoBytesAsync(OnPhotoLoadedListener listener);

	boolean isValid();

	long getDataId();

	int getDefaultPhotoResourceId();
}
