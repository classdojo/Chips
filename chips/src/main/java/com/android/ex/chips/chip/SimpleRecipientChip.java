package com.android.ex.chips.chip;

import android.text.TextUtils;

import com.android.ex.chips.IRecipientEntry;


/**
 * Created by Roman on 4/13/2015.
 */
public class SimpleRecipientChip implements BaseRecipientChip {
	private final CharSequence mDisplay;

	private final CharSequence mValue;

	private final long mDataId;

	private final IRecipientEntry mEntry;

	private boolean mSelected = false;

	private CharSequence mOriginalText;


	public SimpleRecipientChip(final IRecipientEntry entry) {
		mDisplay = entry.getDisplayName();
		mValue = entry.getDestination().trim();
		mDataId = entry.getDataId();
		mEntry = entry;
	}


	@Override
	public boolean isSelected() {
		return mSelected;
	}


	@Override
	public void setSelected(final boolean selected) {
		mSelected = selected;
	}


	@Override
	public CharSequence getDisplay() {
		return mDisplay;
	}


	@Override
	public CharSequence getValue() {
		return mValue;
	}


	@Override
	public long getDataId() {
		return mDataId;
	}


	@Override
	public IRecipientEntry getEntry() {
		return mEntry;
	}


	@Override
	public CharSequence getOriginalText() {
		return !TextUtils.isEmpty(mOriginalText) ? mOriginalText : mEntry.getDestination();
	}


	@Override
	public void setOriginalText(final String text) {
		if(TextUtils.isEmpty(text)) {
			mOriginalText = text;
		} else {
			mOriginalText = text.trim();
		}
	}


	@Override
	public String toString() {
		return mDisplay + " <" + mValue + ">";
	}
}