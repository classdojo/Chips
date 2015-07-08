package com.strv.chipssample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.android.ex.chips.IRecipientEntry;
import com.android.ex.chips.OnPhotoLoadedListener;

import java.io.ByteArrayOutputStream;


/**
 * Created by Roman on 08/07/2015.
 */
public class Cheese implements IRecipientEntry {

	private final int id;
	String name;
	String otherName;
	int iconId;
	private byte[] mPhotoBytes;


	public Cheese(String name, String otherName, int id, int iconId) {
		this.name = name;
		this.otherName = otherName;
		this.id = id;
		this.iconId = iconId;
	}


	@NonNull
	@Override
	public String getDisplayName() {
		return name;
	}


	@NonNull
	@Override
	public String getDestination() {
		return otherName;
	}


	@Override
	public boolean drawPhotos() {
		return true;
	}


	@Override
	public byte[] getPhotoBytes() {
		return mPhotoBytes;
	}


	public void setPhotoBytes(byte[] photoBytes) {
		mPhotoBytes = photoBytes;
	}


	@Override
	public void getPhotoBytesAsync(final OnPhotoLoadedListener listener) {
		Bitmap bitmap = BitmapFactory.decodeResource(SampleApplication.getInstance().getResources(), iconId);
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
		final byte[] byteArray = stream.toByteArray();
		setPhotoBytes(byteArray);
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				listener.onPhotoLoaded(byteArray);
			}
		}, MainFragment.IMAGE_POST_DELAY);

	}


	@Override
	public boolean isValid() {
		return true;
	}


	@Override
	public long getDataId() {
		return id;
	}


	@Override
	public int getDefaultPhotoResourceId() {
		return R.drawable.no_avatar_picture;
	}
}
