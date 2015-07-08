package com.android.ex.chips.chip;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.style.ReplacementSpan;

import com.android.ex.chips.IRecipientEntry;


/**
 * Created by Roman on 4/12/2015.
 */
public class InvisibleRecipientChip extends ReplacementSpan implements DrawableRecipientChip {
	private final SimpleRecipientChip mDelegate;


	public InvisibleRecipientChip(final IRecipientEntry entry) {
		super();

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
	public void draw(final Canvas canvas, final CharSequence text, final int start, final int end,
					 final float x, final int top, final int y, final int bottom, final Paint paint) {
		// Do nothing.
	}


	@Override
	public int getSize(final Paint paint, final CharSequence text, final int start, final int end,
					   final Paint.FontMetricsInt fm) {
		return 0;
	}


	@Override
	public Rect getBounds() {
		return new Rect(0, 0, 0, 0);
	}


	@Override
	public void draw(final Canvas canvas) {
		// do nothing.
	}
}
