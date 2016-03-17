package com.android.ex.chips.chip;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;


/**
 * Created by Martin Petrus on 13. 4. 2015.
 */
public class ChipsUtil {

	public static Bitmap getClip(Bitmap bitmap) {
		Bitmap dstBmp;
		if(bitmap.getHeight() < bitmap.getWidth()) {
			dstBmp = Bitmap.createBitmap(
					bitmap,
					Math.abs(bitmap.getHeight() / 2 - bitmap.getWidth() / 2),
					0,
					bitmap.getHeight(),
					bitmap.getHeight()
			);
		} else {
			dstBmp = Bitmap.createBitmap(
					bitmap,
					0,
					Math.abs(bitmap.getHeight() / 2 - bitmap.getWidth() / 2),
					bitmap.getWidth(),
					bitmap.getWidth()
			);
		}


		int width = dstBmp.getWidth();
		int height = dstBmp.getHeight();
		int diameter = width > height ? width : height;
		int intrinsicWidth = Math.min(width, diameter);
		int intrinsicHeight = Math.min(height, diameter);

		Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

		BitmapShader bitmapShader = new BitmapShader(dstBmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

		Canvas canvas = new Canvas(output);
		Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
		paint.setAntiAlias(true);
		paint.setShader(bitmapShader);

		Paint backgroundPaint = new Paint();
		backgroundPaint.setStyle(Paint.Style.FILL);
		backgroundPaint.setAntiAlias(true);
		backgroundPaint.setColor(Color.WHITE);

		canvas.drawCircle(intrinsicWidth / 2, intrinsicHeight / 2, (diameter / 2), backgroundPaint);
		canvas.drawCircle(intrinsicWidth / 2, intrinsicHeight / 2, (diameter / 2), paint);


		return output;
	}

}
