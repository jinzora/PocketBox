package com.bjdodson.pocketbox.upnp.statemachine;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.teleal.cling.support.avtransport.AVTransportException;
import org.teleal.cling.support.avtransport.impl.state.AbstractState;
import org.teleal.cling.support.avtransport.impl.state.Playing;
import org.teleal.cling.support.model.AVTransport;
import org.teleal.cling.support.model.SeekMode;

import android.media.MediaPlayer;
import android.util.Log;

import com.bjdodson.pocketbox.upnp.MediaRenderer;
import com.bjdodson.pocketbox.upnp.PlaylistManagerService;
import com.bjdodson.pocketbox.upnp.PlaylistManagerService.Playlist;

public class PBPlaying extends Playing<AVTransport> {
	private static final String TAG = "jinzora";
	
    public PBPlaying(AVTransport transport) {
        super(transport);
    }

    @Override
    public void onEntry() {
        super.onEntry();

        Log.d(TAG, "playing track");
        new Thread() {
        	public void run() {
        		try {
                	MediaPlayer player = MediaRenderer.getInstance().getMediaPlayer();
                	player.prepare(); // playback started in onPrepareListener
                } catch (Exception e) {
                	Log.e(TAG, "Error playing track", e);
                }
        	};
        }.start();
    }

    @Override
    public Class<? extends AbstractState> setTransportURI(URI uri, String metaData) {
    	Log.d(TAG, "called Playing::setTransportURI with " + uri);
    	if (!PlaylistManagerService.META_PLAYLIST_CHANGED.equals(metaData)) {
    		PlaylistManagerService pmService = MediaRenderer.getInstance().getPlaylistManager();
    		pmService.setAVTransportURI(getTransport().getInstanceId(), uri.toString(), metaData);
    	}
    	
        return PBPlaying.class;
    }

    @Override
    public Class<? extends AbstractState> stop() {
        // Stop playing!
    	Log.d(TAG, "Playing::stop called");
    	MediaPlayer player = MediaRenderer.getInstance().getMediaPlayer();
    	if (player.isPlaying()) {
    		player.stop();
    	}
        return PBStopped.class;
    }

	@Override
	public Class<? extends AbstractState> next() {
		Log.d(TAG, "Playing::next called");
		return PBTransitionHelpers.next(this, PBPlaying.class);
	}

	@Override
	public Class<? extends AbstractState> pause() {
		Log.d(TAG, "Playing::pause called");
		return PBPlaying.class;
	}

	@Override
	public Class<? extends AbstractState> play(String speed) {
		Log.d(TAG, "Playing::play called");
		return PBPlaying.class;
	}

	@Override
	public Class<? extends AbstractState> previous() {
		Log.d(TAG, "Playing::prev called");
		return PBPlaying.class;
	}

	@Override
	public Class<? extends AbstractState> seek(SeekMode unit, String target) {
		Log.d(TAG, "Playing::seek called");
		return PBPlaying.class;
	}
}