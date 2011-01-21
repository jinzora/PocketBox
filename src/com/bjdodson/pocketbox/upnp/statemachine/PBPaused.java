package com.bjdodson.pocketbox.upnp.statemachine;

import java.net.URI;

import org.teleal.cling.support.avtransport.impl.state.AbstractState;
import org.teleal.cling.support.avtransport.impl.state.PausedPlay;
import org.teleal.cling.support.model.AVTransport;
import org.teleal.cling.support.model.SeekMode;

import android.media.MediaPlayer;
import android.util.Log;

import com.bjdodson.pocketbox.upnp.MediaRenderer;
import com.bjdodson.pocketbox.upnp.PlaylistManagerService;

public class PBPaused extends PausedPlay<AVTransport> {
	private static final String TAG = "jinzora";
	
    public PBPaused(AVTransport transport) {
        super(transport);
    }

    @Override
    public void onEntry() {
        super.onEntry();

        Log.d(TAG, "pausing track");
        MediaPlayer player = MediaRenderer.getInstance().getMediaPlayer();
    	if (player.isPlaying()) {
    		player.pause();
    	}
    }

    @Override
    public Class<? extends AbstractState> setTransportURI(URI uri, String metaData) {
    	Log.d(TAG, "called Playing::setTransportURI with " + uri);
    	if (!PlaylistManagerService.META_PLAYLIST_CHANGED.equals(metaData)) {
    		PlaylistManagerService pmService = MediaRenderer.getInstance().getPlaylistManager();
    		pmService.setAVTransportURI(getTransport().getInstanceId(), uri.toString(), metaData);
    	}
    	
        return PBPaused.class;
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

	public Class<? extends AbstractState> next() {
		Log.d(TAG, "Paused::next called");
		return PBTransitionHelpers.next(this, PBPaused.class);
	}

	public Class<? extends AbstractState> pause() {
		Log.d(TAG, "Paused::pause called");
		return null;
	}

	@Override
	public Class<? extends AbstractState> play(String speed) {
		Log.d(TAG, "Paused::play called");
		MediaPlayer player = MediaRenderer.getInstance().getMediaPlayer();
    	if (!player.isPlaying()) {
    		player.start();
    	}
		return PBPlaying.class;
	}

	public Class<? extends AbstractState> previous() {
		Log.d(TAG, "Paused::prev called");
		return null;
	}

	public Class<? extends AbstractState> seek(SeekMode unit, String target) {
		if (unit.equals(SeekMode.REL_TIME)) {
			MediaPlayer player = MediaRenderer.getInstance().getMediaPlayer();
			player.seekTo(PBTransitionHelpers.timeInMS(target));
		}
		return null;
	}
}