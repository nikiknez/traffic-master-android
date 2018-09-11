package rs.etf.kn153100m.master;

import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import rs.etf.kn153100m.master.model.Configuration;
import rs.etf.kn153100m.master.model.Location;
import rs.etf.kn153100m.master.model.Street;

class FetchTrafficDataTask extends AsyncTask<Void, Void, List<Street>> {

    private MapFeaturesHandler mapFeaturesHandler;
    private RequestQueue queue;

    FetchTrafficDataTask(RequestQueue queue, MapFeaturesHandler mapFeaturesHandler) {
        this.mapFeaturesHandler = mapFeaturesHandler;
        this.queue = queue;
    }

    @Override
    protected List<Street> doInBackground(Void... params) {
        String url = Configuration.SERVER_ADDRESS + "GetTrafficDataServlet";
        try {
            Log.i("FetchTrafficDataTask", "About to request traffic data from the server");
            RequestFuture<JSONObject> future = RequestFuture.newFuture();
            JsonObjectRequest trafficDataRequest = new JsonObjectRequest(Request.Method.GET, url, null, future, future);
            queue.add(trafficDataRequest);
            JSONObject data = future.get();
//            Log.i("FetchTrafficDataTask", "doInBackground: Got data " + data.toString());
            Iterator<String> sources = data.keys();
            List<Street> output = new LinkedList<>();
            while (sources.hasNext()) {
                JSONObject source = data.getJSONObject(sources.next());
                String sourceId = source.getString("id");
                JSONObject sourceData = source.getJSONObject("data");

                Iterator<String> streets = sourceData.keys();
                while (streets.hasNext()) {
                    String streetId = streets.next();
                    JSONObject street = sourceData.getJSONObject(streetId);
                    int intensity = street.getInt("intensity");
                    if (street.has("path")) {
                        JSONArray streetPath = street.getJSONArray("path");
                        Location[] path = new Location[streetPath.length()];
                        for (int i = 0; i < streetPath.length(); i++) {
                            double lat = streetPath.getJSONObject(i).getDouble("lat");
                            double lng = streetPath.getJSONObject(i).getDouble("lng");
                            path[i] = new Location(lat, lng);
                        }
                        output.add(new Street(streetId, path, sourceId, null, null, null, intensity));
                    } else {
                        output.add(new Street(streetId, null, sourceId, null, null, null, intensity));
                    }
                }
            }
            return output;
        } catch (InterruptedException | ExecutionException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(List<Street> output) {
        if (output != null) {
            mapFeaturesHandler.onNewTrafficData(output);
        }
    }
}
