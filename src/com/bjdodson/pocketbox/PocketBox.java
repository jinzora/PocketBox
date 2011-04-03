package com.bjdodson.pocketbox;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import mobisocial.nfc.Nfc;
import mobisocial.nfc.Nfc.NdefHandler;

import org.teleal.cling.support.avtransport.AVTransportException;

import com.bjdodson.pocketbox.RenderingService.IncomingHandler;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class PocketBox extends Activity {
	public static final String TAG = "pocketbox";
    private boolean mIsBound = false;
    private RenderingService mRenderingService;
    private Nfc mNfc;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mNfc = new Nfc(this);
        mNfc.addNdefHandler(mHttpPlaylistHandler);
        mNfc.addNdefHandler(mMimePlaylistHandler);

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
    	mNfc.onResume(this);
    	bindService(new Intent(PocketBox.this, 
	            RenderingService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
	    mIsBound = true;
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	mNfc.onPause(this);
    	if (mIsBound) {
	        // If we have received the service, and hence registered with
	        // it, then now is the time to unregister.

	        // Detach our existing connection.
	        unbindService(mServiceConnection);
	        mIsBound = false;
	    }
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
    	if (mNfc.onNewIntent(this, intent)) return;
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
    
    private NdefHandler mHttpPlaylistHandler = new NdefHandler() {
    	@Override
		public int handleNdef(NdefMessage[] ndefMessages) {
    		// TODO: support full ndef uri specification.
    		NdefRecord record = ndefMessages[0].getRecords()[0];
    		if (record.getTnf() != NdefRecord.TNF_ABSOLUTE_URI) {
    			return NDEF_PROPAGATE;
    		}
    		
    		String url = new String(record.getPayload());
    		if (!url.endsWith(".m3u")) {
    			return NDEF_PROPAGATE;
    		}

    		try {
    			mRenderingService.getMediaRenderer().getPlaylistManager().doPlaylist(new URL(url));
    		} catch (IOException e) {
    			Log.e(TAG, "Could not play playlist", e);
    		}

			return NDEF_CONSUME;
		}
	};
	
	private NdefHandler mMimePlaylistHandler = new NdefHandler() {
		
		@Override
		public int handleNdef(NdefMessage[] ndefMessages) {
			NdefRecord record = ndefMessages[0].getRecords()[0];
			if (record.getTnf() != NdefRecord.TNF_MIME_MEDIA) {
				return NDEF_PROPAGATE;
			}
			
			String type = new String(record.getType());
			if ("audio/mpegurl".equals(type) || "audio/x-mpegurl".equals(type)) {
				try {
					InputStream byteStream = new ByteArrayInputStream(record.getPayload());
	    			mRenderingService.getMediaRenderer().getPlaylistManager().doPlaylist(byteStream);
	    		} catch (IOException e) {
	    			Log.e(TAG, "Could not play playlist", e);
	    		}
				return NDEF_CONSUME;
			}
			return NDEF_PROPAGATE;
		}
	};
    
    private ServiceConnection mServiceConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className,
	            IBinder service) {
	        // This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  We are communicating with our
	        // service through an IDL interface, so get a client-side
	        // representation of that from the raw service object.
	    	RenderingService.LocalBinder binder = ((RenderingService.LocalBinder) service);
	    	mRenderingService = binder.getService();
	        // We want to monitor the service for as long as we are
	        // connected to it.
	    	try {
	            Message msg = Message.obtain(null,
	                    RenderingService.MSG_REGISTER_CLIENT);
	            msg.replyTo = mMessenger;
	            binder.getMessenger().send(msg);
	        } catch (RemoteException e) {
	            // In this case the service has crashed before we could even
	            // do anything with it; we can count on soon being
	            // disconnected (and then reconnected if it can be restarted)
	            // so there is no need to do anything here.
	        }
	    	
	    	onServiceReady();
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        mRenderingService = null;
	    }
	};

	/**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case RenderingService.MSG_PLAYLIST_UPDATED:
            	toast("updated playlist");
            	break;
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    
    public void toast(final String text) {
    	runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(PocketBox.this, text, Toast.LENGTH_SHORT).show();
			}
		});
    }
}