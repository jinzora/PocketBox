package com.bjdodson.pocketbox.upnp.statemachine;

import java.io.IOException;

import org.teleal.cling.support.avtransport.impl.state.AbstractState;

import android.media.MediaPlayer;
import android.util.Log;

import com.bjdodson.pocketbox.upnp.MediaRenderer;
import com.bjdodson.pocketbox.upnp.PlaylistManagerService;
import com.bjdodson.pocketbox.upnp.PlaylistManagerService.Playlist;

public class PBTransitionHelpers {
	public static final String TAG = "jinzora";
	
	public static Class<? extends AbstractState> next(Class<? extends AbstractState> successState) {
		MediaPlayer player = MediaRenderer.getInstance().getMediaPlayer();
    	if (player.isPlaying()) {
    		player.stop();
    	}
    	
    	PlaylistManagerService playlistManager = MediaRenderer.getInstance().getPlaylistManager();
		Playlist playlist = playlistManager.getPlaylist();
		
		playlistManager.advanceCursor(MediaRenderer.getPlayerInstanceId());
		if (playlist.list.size() > playlist.cursor) {
			// TODO: does onEntry() get triggered? or trigger ourselves?
			try {
				String url = playlist.list.get(playlist.cursor).uri;
				player.reset();
		    	player.setDataSource(url);
				return successState;
			} catch (IOException e) {
				Log.w(TAG, "Error on next track", e);
				return PBStopped.class;
			}
		} else {
			return PBStopped.class;
		}
	}
}
