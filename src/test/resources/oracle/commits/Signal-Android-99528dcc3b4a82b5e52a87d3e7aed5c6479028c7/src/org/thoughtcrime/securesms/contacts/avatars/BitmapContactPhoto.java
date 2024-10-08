package org.thoughtcrime.securesms.contacts.avatars;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.makeramen.roundedimageview.RoundedDrawable;

public class BitmapContactPhoto implements ContactPhoto {

  private final Bitmap bitmap;

  BitmapContactPhoto(Bitmap bitmap) {
    this.bitmap = bitmap;
  }

  @Override
  public Drawable asDrawable(Context context) {
    return RoundedDrawable.fromBitmap(bitmap)
                          .setScaleType(ImageView.ScaleType.CENTER_CROP)
                          .setOval(true);
  }
}
