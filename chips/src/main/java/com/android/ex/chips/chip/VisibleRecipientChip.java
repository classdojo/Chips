package com.android.ex.chips.chip;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;

import com.android.ex.chips.IRecipientEntry;


/**
 * Created by Roman on 4/12/2015.
 */
public class VisibleRecipientChip extends ImageSpan implements DrawableRecipientChip {
	private final SimpleRecipientChip mDelegate;


	public VisibleRecipientChip(final Drawable drawable, final IRecipientEntry entry) {
		super(drawable, DynamicDrawableSpan.ALIGN_BOTTOM);

		mDelegate = new SimpleRecipientChip(entry);
	}


	@Override
	public boolean isSelected() {
		return mDelegate.isSelected();
	}


	@Override
	public void setSelected(final boolean selected) {
		mDelegate.setSelected(selected);
	}


	@Override
	public CharSequence getDisplay() {
		return mDelegate.getDisplay();
	}


	@Override
	public CharSequence getValue() {
		return mDelegate.getValue();
	}


	@Override
	public long getDataId() {
		return mDelegate.getDataId();
	}


	@Override
	public IRecipientEntry getEntry() {
		return mDelegate.getEntry();
	}


	@Override
	public CharSequence getOriginalText() {
		return mDelegate.getOriginalText();
	}


	@Override
	public void setOriginalText(final String text) {
		mDelegate.setOriginalText(text);
	}


	@Override
	public Rect getBounds() {
		return getDrawable().getBounds();
	}


	@Override
	public void draw(final Canvas canvas) {
		getDrawable().draw(canvas);
	}


	@Override
	public String toString() {
		return mDelegate.toString();
	}
}
