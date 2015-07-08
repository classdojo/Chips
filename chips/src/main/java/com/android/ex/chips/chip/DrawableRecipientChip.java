package com.android.ex.chips.chip;

import android.graphics.Canvas;
import android.graphics.Rect;


/**
 * Created by Roman on 4/12/2015.
 */
public interface DrawableRecipientChip extends BaseRecipientChip {
	/**
	 * Get the bounds of the chip; may be 0,0 if it is not visibly rendered.
	 */
	Rect getBounds();

	/**
	 * Draw the chip.
	 */
	void draw(Canvas canvas);
}

