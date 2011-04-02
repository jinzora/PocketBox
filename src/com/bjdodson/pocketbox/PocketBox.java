package com.bjdodson.pocketbox;

import java.io.IOException;

import org.teleal.cling.support.avtransport.AVTransportException;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

public class PocketBox extends Activity {
	public static final String TAG = "pocketbox";
    private boolean mIsBound = false;
    private RenderingService mRenderingService;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        findViewById(R.id.prevbutton).setOnClickListener(mPrevButton);
        findViewById(R.id.nextbutton).setOnClickListener(mNextButton);
        findViewById(R.id.playbutton).setOnClickListener(mPlayButton);
        findViewById(R.id.pausebutton).setOnClickListener(mPauseButton);
        findViewById(R.id.clearbutton).setOnClickListener(mClearButton);
        
        // TODO: playlist listview.
        // mRenderingService.getMediaRenderer().getPlaylistManager().getPlaylist().list.size
        // Use Handler.
        // TODO: Convert metadata from m3u to upnp xml in PlaylistManager.
    }
    
    View.OnClickListener mPrevButton = new View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			try {
				mRenderingService.getMediaRenderer()
					.getAVTransportService()
					.previous(mRenderingService.getMediaRenderer().getPlayerInstanceId());
			} catch (AVTransportException e) {
				Log.e(TAG, "Error changing media", e);
			}
		}
	};

    View.OnClickListener mNextButton = new View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			try {
				mRenderingService.getMediaRenderer()
					.getAVTransportService()
					.next(mRenderingService.getMediaRenderer().getPlayerInstanceId());
			} catch (AVTransportException e) {
				Log.e(TAG, "Error changing media", e);
			}
		}
	};
	
    View.OnClickListener mPauseButton = new View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			try {
				mRenderingService.getMediaRenderer()
					.getAVTransportService()
					.pause(mRenderingService.getMediaRenderer().getPlayerInstanceId());
			} catch (AVTransportException e) {
				Log.e(TAG, "Error changing media", e);
			}
		}
	};

    View.OnClickListener mPlayButton = new View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			try {
				mRenderingService.getMediaRenderer()
					.getAVTransportService()
					.play(mRenderingService.getMediaRenderer().getPlayerInstanceId(), null);
			} catch (AVTransportException e) {
				Log.e(TAG, "Error changing media", e);
			}
		}
	};
	
	View.OnClickListener mClearButton = new View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			mRenderingService.getMediaRenderer()
				.getPlaylistManager()
				.clear(mRenderingService.getMediaRenderer().getPlayerInstanceId());
			
		}
	};
	
    @Override
    protected void onResume() {
    	super.onResume();
    	bindService(new Intent(PocketBox.this, 
	            RenderingService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
	    mIsBound = true;
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	
    	if (mIsBound) {
	        // If we have received the service, and hence registered with
	        // it, then now is the time to unregister.

	        // Detach our existing connection.
	        unbindService(mServiceConnection);
	        mIsBound = false;
	    }
    }
    
    public void onServiceReady() {
		if (getIntent() != null && !Intent.ACTION_VIEW.equals(getIntent().getAction())) {
			return;
		}

		Uri playlistUri = getIntent().getData();
		try {
			Log.d(TAG, "sending playlist over");
			mRenderingService.getMediaRenderer().getPlaylistManager().doPlaylist(getContentResolver(), playlistUri);
			setIntent(null);
		} catch (IOException e) {
			Log.e(TAG, "Error playing playlist", e);
		}
    }
    
    private ServiceConnection mServiceConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className,
	            IBinder service) {
	        // This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  We are communicating with our
	        // service through an IDL interface, so get a client-side
	        // representation of that from the raw service object.
	    	mRenderingService = ((RenderingService.LocalBinder) service).getService();

	        // We want to monitor the service for as long as we are
	        // connected to it.
	        /*
	    	try {
	            Message msg = Message.obtain(null,
	                    BlueService.MSG_REGISTER_CLIENT);
	            msg.replyTo = mMessenger;
	            mService.send(msg);
	        } catch (RemoteException e) {
	            // In this case the service has crashed before we could even
	            // do anything with it; we can count on soon being
	            // disconnected (and then reconnected if it can be restarted)
	            // so there is no need to do anything here.
	        }
	        */
	    	
	    	onServiceReady();
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        mRenderingService = null;
	    }
	};
}