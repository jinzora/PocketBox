package com.bjdodson.pocketbox.upnp.statemachine;

import java.io.IOException;
import java.net.URI;

import org.teleal.cling.support.avtransport.impl.state.AbstractState;
import org.teleal.cling.support.avtransport.lastchange.AVTransportVariable;
import org.teleal.cling.support.model.AVTransport;
import org.teleal.cling.support.model.MediaInfo;
import org.teleal.cling.support.model.PositionInfo;

import android.media.MediaPlayer;
import android.util.Log;

import com.bjdodson.pocketbox.upnp.MediaRenderer;
import com.bjdodson.pocketbox.upnp.PlaylistManagerService;
import com.bjdodson.pocketbox.upnp.PlaylistManagerService.Playlist;

public class PBTransitionHelpers {
	public static final String TAG = "jinzora";
	
	public static Class<? extends AbstractState> next(AbstractState state, Class<? extends AbstractState> successState) {
		MediaPlayer player = MediaRenderer.getInstance().getMediaPlayer();
    	if (player.isPlaying()) {
    		player.stop();
    	}
    	
    	PlaylistManagerService playlistManager = MediaRenderer.getInstance().getPlaylistManager();
		Playlist playlist = playlistManager.getPlaylist();
		
		playlistManager.advanceCursor(MediaRenderer.getPlayerInstanceId());
		if (playlist.list.size() > playlist.cursor) {
			try {
				String url = playlist.list.get(playlist.cursor).uri;
				player.reset();
		    	player.setDataSource(url);
		    	
		    	String metaData = playlist.list.get(playlist.cursor).metadata;
				URI uri = URI.create(url);
		    	setTrackDetails(state.getTransport(), uri, metaData);
				return successState;
			} catch (IOException e) {
				Log.w(TAG, "Error on next track", e);
				return PBStopped.class;
			}
		} else {
			return PBStopped.class;
		}
	}
	
	public static void setTrackDetails(AVTransport transport, URI uri, String metaData) {
		transport.setMediaInfo(
                new MediaInfo(uri.toString(), metaData)
        );
    	
    	transport.setPositionInfo(
                new PositionInfo(1, metaData, uri.toString())
        );
    	
        transport.getLastChange().setEventedValue(
                transport.getInstanceId(),
                new AVTransportVariable.AVTransportURI(uri),
                new AVTransportVariable.CurrentTrackURI(uri)
        );
	}
}
