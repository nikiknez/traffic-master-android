package rs.etf.kn153100m.master;

import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

import rs.etf.kn153100m.master.model.Configuration;

class FetchConfigTask extends AsyncTask<Void, Configuration, Configuration> {

    private MapFeaturesHandler mapFeaturesHandler;
    private RequestQueue queue;

    FetchConfigTask(RequestQueue queue, MapFeaturesHandler mapFeaturesHandler) {
        this.mapFeaturesHandler = mapFeaturesHandler;
        this.queue = queue;
    }

    @Override
    protected Configuration doInBackground(Void... params) {
        String url = Configuration.SERVER_ADDRESS + "GetConfigurationServlet";
        try {
            Log.i("FetchConfigTask", "About to request configuration from the server");
            RequestFuture<JSONObject> future = RequestFuture.newFuture();
            JsonObjectRequest configRequest = new JsonObjectRequest(Request.Method.GET, url, null, future, future);
            queue.add(configRequest);

            JSONObject data = future.get();
//            Log.i("FetchConfigTask", "doInBackground: Got data " + data.toString());
            return new Gson().fromJson(data.toString(), Configuration.class);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Configuration... values) {
        mapFeaturesHandler.updateConfig(values[0]);
    }

    @Override
    protected void onPostExecute(Configuration c) {
        if (c != null) {
            mapFeaturesHandler.updateConfig(c);
        }
    }
}
