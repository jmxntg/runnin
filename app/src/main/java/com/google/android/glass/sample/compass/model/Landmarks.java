/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.glass.sample.compass.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.google.android.glass.sample.compass.R;
import com.google.android.glass.sample.compass.util.MathUtils;

/**
 * This class provides access to a list of hard-coded landmarks (located in
 * {@code res/raw/landmarks.json}) that will appear on the compass when the user is near them.
 */
public class Landmarks {

    private static final String TAG = Landmarks.class.getSimpleName();

    /**
     * The threshold used to display a landmark on the compass.
     */
    private static final double MAX_DISTANCE_KM = 10;

    /**
     * The list of landmarks loaded from resources.
     */
    private final ArrayList<Place> mPlaces;
    
    //private Vector<Point> track;
    private Vector<Profile> profiles;

    //private final Handler mHandler;

    /**
     * Initializes a new {@code Landmarks} object by loading the landmarks from the resource
     * bundle.
     */
    public Landmarks(Context context) {
        mPlaces = new ArrayList<Place>();

        // This class will be instantiated on the service's main thread, and doing I/O on the
        // main thread can be dangerous if it will block for a noticeable amount of time. In
        // this case, we assume that the landmark data will be small enough that there is not
        // a significant penalty to the application. If the landmark data were much larger,
        // we may want to load it in the background instead.
        //String jsonString = readLandmarksResource(context, R.raw.landmarks);
        //populatePlaceList(jsonString);

        //mHandler = new Handler();
        //DownloadFilesTask dft = new DownloadFilesTask(mLandmarks, mCompassView);

        Thread mTask = new Thread() {
            public void run() {
                populateTrackList(null);
            }
        };
        //mHandler.postDelayed(mTask, 0);
        mTask.start();
    }

    /**
     * Gets a list of landmarks that are within ten kilometers of the specified coordinates. This
     * function will never return null; if there are no locations within that threshold, then an
     * empty list will be returned.
     */
    public List<Place> getNearbyLandmarks(double latitude, double longitude) {
        ArrayList<Place> nearbyPlaces = new ArrayList<Place>();

        for (Place knownPlace : mPlaces) {
            if (MathUtils.getDistance(latitude, longitude,
                    knownPlace.getLatitude(), knownPlace.getLongitude()) <= MAX_DISTANCE_KM) {
                nearbyPlaces.add(knownPlace);
            }
        }

        return nearbyPlaces;
    }
    
    public List<Place> getNearbyLandmarks2(int t) {
        ArrayList<Place> nearbyPlaces = new ArrayList<Place>();
        for(int i = 0; i < profiles.size(); i++) {
        	Point p = getPoint2(profiles.elementAt(i).track, t);
            nearbyPlaces.add(new Place(p.lat, p.lon, "id" + profiles.elementAt(i).id));	
        }
        return nearbyPlaces;
    }

    /**
     * Populates the internal places list from places found in a JSON string. This string should
     * contain a root object with a "landmarks" property that is an array of objects that represent
     * places. A place has three properties: name, latitude, and longitude.
     */
    private void populatePlaceList(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            JSONArray array = json.optJSONArray("landmarks");

            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    JSONObject object = array.optJSONObject(i);
                    Place place = jsonObjectToPlace(object);
                    if (place != null) {
                        mPlaces.add(place);
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Could not parse landmarks JSON string", e);
        }
    }

    /**
     * Converts a JSON object that represents a place into a {@link Place} object.
     */
    private Place jsonObjectToPlace(JSONObject object) {
        String name = object.optString("name");
        double latitude = object.optDouble("latitude", Double.NaN);
        double longitude = object.optDouble("longitude", Double.NaN);

        if (!name.isEmpty() && !Double.isNaN(latitude) && !Double.isNaN(longitude)) {
            return new Place(latitude, longitude, name);
        } else {
            return null;
        }
    }

    /**
     * Reads the text from {@code res/raw/landmarks.json} and returns it as a string.
     */
    private static String readLandmarksResource(Context context, int id) {
        InputStream is = context.getResources().openRawResource(id);
        StringBuffer buffer = new StringBuffer();

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
                buffer.append('\n');
            }
        } catch (IOException e) {
            Log.e(TAG, "Could not read landmarks resource", e);
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(TAG, "Could not close landmarks resource stream", e);
                }
            }
        }

        return buffer.toString();
    }
    
    public static Point getPoint2(Vector<Point> track, int t) {
    	
    	Point previous, current;
    	current = track.elementAt(0);
    	
    	for(int i = 0; i < track.size(); i++) {
    		previous = current;
    		current = track.elementAt(i);
    		
    		if(current.timeInSeconds == t) return current;
    		else if(previous.timeInSeconds <= t && t <= current.timeInSeconds) {
    			double deltaLat = current.lat - previous.lat;
    			double deltaLon = current.lon - previous.lon;
    			double time = (t - previous.timeInSeconds)/(current.timeInSeconds - previous.timeInSeconds);
    			
    			Point p = new Point();
    			p.timeInSeconds = t;
    			p.lat = previous.lat + time * deltaLat;
    			p.lon = previous.lon + time * deltaLon;
    			return p;
    			
    		}
    	}
    	Point p = track.elementAt(track.size()-1) ;
    	p.timeInSeconds = t;
    	return p;
    }
    
    public static Point getPoint(Vector<Point> track, int t) {

		Point previous, current;
		current = track.elementAt(0);
		previous = current;
		
		int i;
		for(i = 0; i < track.size(); i++) {
			previous = current;
			if(track.elementAt(i).timeInSeconds == t) {
				return track.elementAt(i);
			} else if(track.elementAt(i).timeInSeconds > t) {
				current = track.elementAt(i);
				break;
			}
		}
		
		if(i == track.size()) {
			previous = current;
		}
		
		Point p = new Point();
		double delta = (t - previous.timeInSeconds);
		double diff = current.timeInSeconds - previous.timeInSeconds;
		
		if(diff != 0) {
			delta /= diff;
		}
		
		p.lat = previous.lat + (current.lat - previous.lat) * delta;
		p.lon = previous.lon + (current.lon - previous.lon) * delta;
		p.timeInSeconds = t;
		
		return p;
	}

	public String getHTML(String urlToRead) {
		URL url;
		HttpURLConnection conn;
		BufferedReader rd;
		String line;
		StringBuilder result = new StringBuilder();
		try {
			url = new URL(urlToRead);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			rd.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result.toString();
	}
    
    private String downloadTrack(String url) {
    	StringBuffer sb = new StringBuffer();
    	
    	try {
    	    // Create a URL for the desired page
    	    URL url2 = new URL(url);

    	    // Read all the text returned by the server
    	    BufferedReader in = new BufferedReader(new InputStreamReader(url2.openStream()));
    	    String str;
    	    while ((str = in.readLine()) != null) {
    	        // str is one line of text; readLine() strips the newline character(s)
    	    	sb.append(str);
    	    }
    	    in.close();
    	} catch (MalformedURLException e) {
    	} catch (IOException e) {
    	}
    	return sb.toString();
    }
    
    private void populateTrackList(Context context) {
    	
    	// only supports the first track segment of a runkeeper exported file
    	
    	profiles = new Vector<Profile>();
    	
    	int ids[] = {R.raw.track201506190702, R.raw.track201507130651};
    	String urls[] = {"http://www.cin.ufpe.br/~jmxnt/sportshack/track201506190702.gpx", "http://www.cin.ufpe.br/~jmxnt/sportshack/track201507130651.gpx"};
    	
    	for(int i = 0; i < ids.length; i++) {
    		Profile profile = new Profile();
    		profile.id = i;
    		profile.track = new Vector<Point>();
        	Date startDate = null;
        	
        	//String content = readLandmarksResource(context, ids[i]);
        	//String content = downloadTrack(urls[i]);
            String content = getHTML(urls[i]);

        	StringTokenizer st = new StringTokenizer(content, " =<>\"");
        	String temp = null;
        	while(st.hasMoreTokens()) {
        		temp = st.nextToken();
        		if(temp.equals("trk")) {
        			while(st.hasMoreTokens()) {
        				temp = st.nextToken();
        				if(temp.equals("time")) {
        					temp = st.nextToken();
        					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    						try {
    							startDate = format.parse(temp);
    						} catch (ParseException e) {
    							e.printStackTrace();
    						}
    						break;
        				}
        			}
        		} else if(temp.equals("trkpt")) {
        			Point p = new Point();
        			while(st.hasMoreTokens()) {
        	    		temp = st.nextToken();
        	    		if(temp.equals("lat")) {
        	    			temp = st.nextToken();
        	    			//temp = temp.replace(".", ",");
        	    			//System.out.println(temp);
        	    			p.lat = Double.parseDouble(temp);
        	    		} else if(temp.equals("lon")) {
        	    			p.lon = Double.parseDouble(st.nextToken());
        	    		} else if(temp.equals("time")) {
        	    			temp = st.nextToken();
        	    			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        	    			Date parsed = null;
    						try {
    							parsed = format.parse(temp);
    						} catch (ParseException e) {
    							// TODO Auto-generated catch block
    							e.printStackTrace();
    						}
        	    			p.timeInSeconds = (parsed.getTime() - startDate.getTime())/1000;
        	    			
        	    		} else if(temp.equals("/trkpt")) {
        	    			profile.track.add(p);
        	    			break;
        	    		}
        			}
        		} else if(temp.equals("/trkseg")) {
        			break;
        		}
        	}
        	//System.out.println("Quantidade de pontos no xml: " + track.size());
//        	for(int i = 0; i < track.size(); i++) {
//        		System.out.println(track.elementAt(i));
//        	}
//        	for(int i = 0; i < 800; i++) {
//        		Point p = getPoint2(track, i);
//        		System.out.println(p);
//        	}    		
    		
    		profiles.add(profile);
    	}
    }
}

class Profile {
	
	Vector<Point> track;
	int id;
	
}

class Point {
	double lat, lon, timeInSeconds;
	
	public Point() {
		this.lat = 0;
		this.lon = 0;
		this.timeInSeconds = 0;
	}
	
//	public Point(float lat, float lon) {
//		this.lat = lat;
//		this.lon = lon;
//	}
	
	public String toString() {
		//return "lat: " + lat + " lon: " + lon + " time: (" + timeInSeconds + "s)"; 
		return lat + " " + lon;
	}
}
