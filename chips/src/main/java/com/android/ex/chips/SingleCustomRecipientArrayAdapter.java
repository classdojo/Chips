package com.android.ex.chips;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.ex.chips.chip.ChipsUtil;


/**
 * Created by Roman on 4/12/2015.
 */
public final class SingleCustomRecipientArrayAdapter extends ArrayAdapter<IRecipientEntry> {
	private final OnDeleteItemListener mListener;
	private final LayoutInflater mLayoutInflater;
	private int mLayoutId;


	public SingleCustomRecipientArrayAdapter(Context context, int resourceId, IRecipientEntry entry) {
		this(context, resourceId, entry, null);
	}


	public SingleCustomRecipientArrayAdapter(Context context, int resourceId, IRecipientEntry entry, OnDeleteItemListener listener) {
		super(context, resourceId, new IRecipientEntry[]{entry});
		mLayoutInflater = LayoutInflater.from(context);
		mLayoutId = resourceId;
		mListener = listener;
	}


	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(convertView == null)
			convertView = newView();
		bindView(convertView, getItem(position));
		return convertView;
	}


	private View newView() {
		return mLayoutInflater.inflate(mLayoutId, null);
	}


	private void bindView(View view, final IRecipientEntry entry) {
		TextView display = (TextView) view.findViewById(android.R.id.title);
		ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
		display.setText(entry.getDisplayName());
		display.setVisibility(View.VISIBLE);
		if(entry.drawPhotos()) {
			imageView.setVisibility(View.VISIBLE);
			byte[] photoBytes;
			photoBytes = entry.getPhotoBytes();
			if(photoBytes != null) {
				Bitmap bitmap = ChipsUtil.getClip(BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.length));
				imageView.setImageBitmap(bitmap);
			}
		} else {
			imageView.setVisibility(View.GONE);
		}

		ImageView delete = (ImageView) view.findViewById(android.R.id.icon1);

		if(mListener != null) {
			delete.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					{
						mListener.onDeleteItem(v, entry);
					}
				}
			});
		}

		TextView destination = (TextView) view.findViewById(android.R.id.text1);
		//        destination.setText(Rfc822Tokenizer.tokenize(entry.getDestination())[0].getAddress());
		destination.setText(entry.getDestination());
	}


	public interface OnDeleteItemListener {
		void onDeleteItem(View view, IRecipientEntry entry);
	}
}
