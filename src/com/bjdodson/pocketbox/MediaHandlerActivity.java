package com.bjdodson.pocketbox;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import com.bjdodson.pocketbox.upnp.MediaRenderer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class MediaHandlerActivity extends Activity {
	public static final String TAG = "pocketbox";
	public static final String UNKNOWN_TRACK = "Unknown Track";
	private MediaRenderer mediaRenderer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mediaRenderer = MediaRenderer.getInstance();

		StringBuilder playlistBuilder = new StringBuilder();
		try {
			Uri playlistUri = getIntent().getData();
			InputStream playlistInput = getContentResolver().openInputStream(playlistUri);
			BufferedReader br = new BufferedReader(new InputStreamReader(playlistInput));
			String line = null; 
			String lastLine = null;
			
			line = br.readLine();
			while (line != null) {
				if (line.length() > 0 && line.charAt(0) != '#') {
					try {
						URL track = new URL(line);
						String trackname;
						
					    if (lastLine.charAt(0) == '#') {
					    	int pos;
					    	if (-1 != (pos = lastLine.indexOf(','))) {
					    		trackname = lastLine.substring(pos+1,lastLine.length());
					    	} else {
					    		trackname = UNKNOWN_TRACK;
					    	}
					    } else {
					    	trackname = UNKNOWN_TRACK;
					    }
					    
					    try {
						    Log.d(TAG, "playing " + track + " on " + mediaRenderer);
						    mediaRenderer.getAVTransportService()
						    	.setAVTransportURI(mediaRenderer.getPlayerInstanceId(), track.toString(), null);
					    } catch (Exception e) {
					    	Log.e(TAG, "Error playing track", e);
					    }
					    break;
					} catch (Exception e) {
						// probably a comment line
					}
				}
				
				lastLine = line;
				line = br.readLine();
			}
		} catch (Exception e) {
			Log.e(TAG, "Error reading playlist", e);
			return;
		}
		
		
	}
}
