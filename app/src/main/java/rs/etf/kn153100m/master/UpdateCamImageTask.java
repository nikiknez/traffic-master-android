package rs.etf.kn153100m.master;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.gms.maps.model.Marker;

import java.net.URL;

class UpdateCamImageTask extends AsyncTask<String, Bitmap, Void> {

    private ImageView imageView;
    private Marker marker;

    UpdateCamImageTask(ImageView imageView, Marker marker) {
        this.imageView = imageView;
        this.marker = marker;
    }

    @Override
    protected Void doInBackground(String... params) {
        try {
            URL url = new URL(params[0]);
            Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            publishProgress(bmp);
        } catch (Exception e) {
            Log.w("UpdateCamImageTask", "doInBackground: cancelled fatching of cam image");
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Bitmap... values) {
        imageView.setImageBitmap(values[0]);
        marker.showInfoWindow();
    }
}
