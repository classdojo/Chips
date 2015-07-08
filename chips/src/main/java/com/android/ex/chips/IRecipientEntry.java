package com.android.ex.chips;

import android.support.annotation.NonNull;


/**
 * Created by Roman on 4/12/2015.
 */
public interface IRecipientEntry {
	@NonNull String getDisplayName();

	@NonNull String getDestination();

	boolean drawPhotos();

	byte[] getPhotoBytes();

	void getPhotoBytesAsync(OnPhotoLoadedListener listener);

	boolean isValid();

	long getDataId();

	int getDefaultPhotoResourceId();
}
