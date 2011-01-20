package com.bjdodson.pocketbox.upnp.statemachine;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

import org.teleal.cling.model.types.UnsignedIntegerFourBytes;
import org.teleal.cling.support.avtransport.impl.state.AbstractState;
import org.teleal.cling.support.avtransport.impl.state.Stopped;
import org.teleal.cling.support.model.AVTransport;
import org.teleal.cling.support.model.SeekMode;

import android.media.MediaPlayer;
import android.util.Log;

import com.bjdodson.pocketbox.upnp.MediaRenderer;
import com.bjdodson.pocketbox.upnp.PlaylistManagerService;
import com.bjdodson.pocketbox.upnp.PlaylistManagerService.Playlist;

public class PBStopped extends Stopped<AVTransport> {
	private static final String TAG = "jinzora";
	
    public PBStopped(AVTransport transport) {
        super(transport);
    }

    public void onEntry() {
        super.onEntry();
        Log.d(TAG, "entered stop state");
        // Optional: Stop playing, release resources, etc.
    }

    public void onExit() {
    	Log.d(TAG, "exit stop state");
        // Optional: Cleanup etc.
    }

    @Override
    public Class<? extends AbstractState> setTransportURI(URI uri, String metaData) {
    	Log.d(TAG, "called stop::setTransportURI");
    	if (!PlaylistManagerService.META_PLAYLIST_CHANGED.equals(metaData)) {
    		UnsignedIntegerFourBytes instanceId = getTransport().getInstanceId();
    		PlaylistManagerService pmService = MediaRenderer.getInstance().getPlaylistManager();
    		pmService.setAVTransportURI(instanceId, uri.toString(), metaData);
    		int position = pmService.getLength(instanceId) - 1;
    		pmService.jumpTo(instanceId, position);
    		
    		AVTransport transport = getTransport();
	    	PBTransitionHelpers.setTrackDetails(transport, uri, metaData);
	    	
    		try {
	    		MediaPlayer player = MediaRenderer.getInstance().getMediaPlayer();
	    		Playlist playlist = pmService.getPlaylist();
	    		String url = playlist.list.get(playlist.cursor).uri;
				player.reset();
		    	player.setDataSource(url);
    		} catch (IOException e) {
    			Log.e(TAG, "Error setting next track");
    			return PBNoMediaPresent.class;
    		}
    	}
    	
        return PBStopped.class;
    }

    @Override
    public Class<? extends AbstractState> stop() {
    	Log.d(TAG, "called stop::stop");
        return null;
    }

    @Override
    public Class<? extends AbstractState> play(String speed) {
    	Log.d(TAG, "called stop::play");
        // It's easier to let this classes' onEntry() method do the work
        return PBPlaying.class;
    }

    @Override
    public Class<? extends AbstractState> next() {
    	Log.d(TAG, "called stop::next");
    	return PBTransitionHelpers.next(this, PBStopped.class);
    }

    @Override
    public Class<? extends AbstractState> previous() {
    	Log.d(TAG, "called stop::prev");
        return null;
    }

    @Override
    public Class<? extends AbstractState> seek(SeekMode unit, String target) {
    	if (unit.equals(SeekMode.REL_TIME)) {
			MediaPlayer player = MediaRenderer.getInstance().getMediaPlayer();
			player.seekTo(PBTransitionHelpers.timeInMS(target));
		}
		return null;
    }
}