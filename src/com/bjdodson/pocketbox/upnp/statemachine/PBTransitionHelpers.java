package com.bjdodson.pocketbox.upnp.statemachine;

import java.io.IOException;
import java.net.URI;

import org.teleal.cling.support.avtransport.impl.state.AbstractState;
import org.teleal.cling.support.avtransport.lastchange.AVTransportVariable;
import org.teleal.cling.support.contentdirectory.DIDLParser;
import org.teleal.cling.support.model.AVTransport;
import org.teleal.cling.support.model.DIDLContent;
import org.teleal.cling.support.model.MediaInfo;
import org.teleal.cling.support.model.PositionInfo;
import org.teleal.cling.support.model.Res;
import org.teleal.cling.support.model.item.Item;

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
    	
		long track = 1;
		String duration = "00:00:00";
		String relTime = "00:00:00";
		String absTime = "00:00:00";
		int relCount = Integer.MAX_VALUE;
		int absCount = Integer.MAX_VALUE;
		
		try {
			// Exception means no track info available.
			DIDLContent didl = new DIDLParser().parse(metaData);
			Item item = didl.getItems().get(0);
			Res res = item.getFirstResource();
			duration = res.getDuration();
		} catch (Exception e) {}
		
		transport.setPositionInfo(
                new PositionInfo(track, duration, metaData,
                		uri.toString(), relTime, absTime, relCount, absCount)
        );
    	
        transport.getLastChange().setEventedValue(
                transport.getInstanceId(),
                new AVTransportVariable.AVTransportURI(uri),
                new AVTransportVariable.CurrentTrackURI(uri)
        );
	}
}
