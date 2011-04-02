package com.bjdodson.pocketbox.upnp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.teleal.cling.binding.annotations.UpnpAction;
import org.teleal.cling.binding.annotations.UpnpInputArgument;
import org.teleal.cling.binding.annotations.UpnpService;
import org.teleal.cling.binding.annotations.UpnpServiceId;
import org.teleal.cling.binding.annotations.UpnpServiceType;
import org.teleal.cling.binding.annotations.UpnpStateVariable;
import org.teleal.cling.binding.annotations.UpnpStateVariables;
import org.teleal.cling.model.types.UnsignedIntegerFourBytes;
import org.teleal.cling.support.avtransport.AVTransportException;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

@UpnpService(
        serviceId = @UpnpServiceId(namespace = "jinzora.org", value = "PlaylistManager"),
        serviceType = @UpnpServiceType(namespace = "jinzora.org", value = "PlaylistManager", version = 1)
)

@UpnpStateVariables({
	@UpnpStateVariable(
		name = "AVTransportURI",
		sendEvents = false,
		datatype = "string"),

	@UpnpStateVariable(
		name = "AVTransportURIMetaData",
		sendEvents = false,
		datatype = "string",
		defaultValue = "NOT_IMPLEMENTED"),
		
	@UpnpStateVariable(
		name = "A_ARG_TYPE_InstanceID",
		sendEvents = false,
		datatype = "ui4")
})

public class PlaylistManagerService {
	public static final String UNKNOWN_TRACK = "Unknown Track";
	private static final String TAG = "pocketbox";
	public static final String META_PLAYLIST_CHANGED = "NOTIFY_AVTRANSPORT_SERVICE";
	private static UnsignedIntegerFourBytes sInstanceId = new UnsignedIntegerFourBytes(0);
	private MediaRenderer mMediaRenderer;
	
	public PlaylistManagerService(MediaRenderer mediaRenderer) {
		mMediaRenderer = mediaRenderer;
	}

	public static UnsignedIntegerFourBytes getPlayerInstanceId() {
		return sInstanceId;
	}

	// Local actions
	public void doPlaylist(URL url) throws IOException {
		URLConnection urlConnection = url.openConnection();
		doPlaylist(urlConnection.getInputStream());
	}
	
	public void doPlaylist(ContentResolver resolver, Uri playlistUri) throws IOException {
		InputStream playlistInput = resolver.openInputStream(playlistUri);
		doPlaylist(playlistInput);
	}
	
	public void doPlaylist(InputStream playlistInput) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(playlistInput));
		String line = null; 
		String lastLine = null;
		boolean trackAdded = false;
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
					    setAVTransportURI(sInstanceId, track.toString(), null);
					    getPlaylist().add(track.toString(), null);
					    trackAdded = true;
				    } catch (Exception e) {
				    	Log.e(TAG, "Error playing track", e);
				    }
				} catch (Exception e) {
					// probably a comment line
				}
			}
			
			lastLine = line;
			line = br.readLine();
		}
		if (trackAdded) {
			try {
				// TODO: only play if we are stopped / paused.
				mMediaRenderer.getAVTransportService().play(MediaRenderer.getPlayerInstanceId(), null);
			} catch (AVTransportException e) {
				Log.e(TAG, "Error playing media", e);
			}
		}
	}
	
	@UpnpAction
    public void clear(@UpnpInputArgument(name = "InstanceID") UnsignedIntegerFourBytes instanceId) {
		Log.d(TAG, "playlistManager::clear called");
		try {
			if (mPlaylists.containsKey(instanceId)) {
				mPlaylists.get(instanceId).clear();
				MediaRenderer.getInstance().getAVTransportService()
					.stop(instanceId);
			}
		} catch (AVTransportException e) {
			Log.w(TAG, "Error stopping player", e);
		}
    }
	
	@UpnpAction
	public void setAVTransportURI(
			@UpnpInputArgument(name = "InstanceID") UnsignedIntegerFourBytes instanceId,
			@UpnpInputArgument(name = "CurrentURI", stateVariable = "AVTransportURI") String currentURI,
		    @UpnpInputArgument(name = "CurrentURIMetaData", stateVariable = "AVTransportURIMetaData") String currentURIMetaData) {
		
		Log.d(TAG, "Called setURI with " + currentURI);
		if (!mPlaylists.containsKey(instanceId)) {
			mPlaylists.put(instanceId, new Playlist());
		}
		mPlaylists.get(instanceId).add(currentURI, currentURIMetaData);
		
		// Notify the AVTransportService in case this alters our machine state.
		try {
			MediaRenderer.getInstance().getAVTransportService()
				.setAVTransportURI(instanceId, currentURI, META_PLAYLIST_CHANGED);
		} catch (AVTransportException e) {
			Log.e(TAG, "Error notifying of playlist update", e);
		}
	}
	
	public void setCursor(UnsignedIntegerFourBytes instanceId, int position) {
		Playlist p = mPlaylists.get(instanceId);
		if (p == null) {
			return;
		}
		p.cursor = position;
	}
	
	public void advanceCursor(UnsignedIntegerFourBytes instanceId) {
		Playlist p = mPlaylists.get(instanceId);
		if (p == null) {
			return;
		}
		p.cursor++;
	}
	
	public void jumpTo(UnsignedIntegerFourBytes instanceId, int position) {
		Playlist p = mPlaylists.get(instanceId);
		if (p == null) {
			return;
		}
		Log.d(TAG, "jumping to " + position);
		p.cursor = position;
	}
	
	public int getLength(UnsignedIntegerFourBytes instanceId) {
		Playlist p = mPlaylists.get(instanceId);
		if (p == null) {
			return -1;
		}
		return p.list.size();
	}
	
	// next/prev/play/pause managed by AVTransport.
	
	// add:
	// queueToEnd, queueNext, replaceWith(?)
	// support single items, playlists, and multiple items.

	public Playlist getPlaylist() {
		return mPlaylists.get(getPlayerInstanceId());
	}
	
	Map<UnsignedIntegerFourBytes, Playlist> mPlaylists = new ConcurrentHashMap<UnsignedIntegerFourBytes, Playlist>();
	
	public class Playlist {
		public static final int FIRST_ENTRY = 0;
		public List<PlaylistEntry> list;
		public int cursor;
		
		public Playlist() {
			list = new ArrayList<PlaylistEntry>();
			cursor = FIRST_ENTRY;
		}
		
		public void add(String uri, String metadata) {
			// TODO: if playlist is an m3u, get its entries.
			PlaylistEntry e = new PlaylistEntry(uri, metadata);
			list.add(e);
		}
		
		public void clear() {
			list.clear();
		}
	}
	
	public class PlaylistEntry {
		public String uri;
		public String metadata;
		
		public PlaylistEntry(String uri, String metadata) {
			this.uri = uri;
			this.metadata = metadata;
		}
	}
}
