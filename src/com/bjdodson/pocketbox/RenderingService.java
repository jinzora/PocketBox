package com.bjdodson.pocketbox;

import java.io.IOException;
import java.net.URI;

import org.teleal.cling.android.AndroidUpnpServiceImpl;
import org.teleal.cling.binding.LocalServiceBindingException;
import org.teleal.cling.model.ValidationError;
import org.teleal.cling.model.ValidationException;
import org.teleal.cling.model.meta.DeviceDetails;
import org.teleal.cling.model.meta.DeviceIdentity;
import org.teleal.cling.model.meta.Icon;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.types.DeviceType;
import org.teleal.cling.model.types.UDADeviceType;
import org.teleal.cling.model.types.UDN;
import org.teleal.cling.model.types.UnsignedIntegerFourBytes;
import org.teleal.cling.support.avtransport.AVTransportException;
import org.teleal.cling.support.avtransport.impl.AVTransportService;

import com.bjdodson.pocketbox.upnp.MediaRenderer;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/**
 * Connects to the UPnP backend and maintains the MediaPlayer.
 */
public class RenderingService extends AndroidUpnpServiceImpl {
	private String TAG = "jinzora";
	private boolean mStarted;
	private WifiLock mWifiLock;
	
	private final MediaRenderer mMediaRenderer = MediaRenderer.getInstance();
	private final MediaPlayer mMediaPlayer = new MediaPlayer();
	private final UnsignedIntegerFourBytes PLAYER_INSTANCE_ID = MediaRenderer.getPlayerInstanceId();

	LocalDevice createDevice() throws ValidationException,
			LocalServiceBindingException, IOException {

		DeviceType type = new UDADeviceType("MediaRenderer", 1);
		DeviceIdentity identity = new DeviceIdentity(UDN.uniqueSystemIdentifier("PocketBox Renderer v1"));
		DeviceDetails details = mMediaRenderer.getDeviceDetails();

		URI uri = null;
		try {
			uri = new URI("assets/icon.png");
		} catch (Exception e) {}
		Icon icon = new Icon("image/png", 48, 48, 8, uri, getResources().getAssets().open("icon.png"));

		return new LocalDevice(identity, type, details, icon, MediaRenderer.getServices());
	}

	@Override
	public IBinder onBind(Intent arg0) {
		if (!mStarted) {
			startService(new Intent(RenderingService.this, RenderingService.class));
		}
		//return mMessenger.getBinder();
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		initializeMediaPlayer();
		mMediaRenderer.setMediaPlayer(mMediaPlayer);
		
		WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		mWifiLock = wifiManager.createWifiLock(TAG);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (!mStarted) {
			mWifiLock.acquire();
			try {
				upnpService.getRegistry().addDevice(createDevice());
				
				mStarted = true;
				Toast.makeText(this, "Started rendering service.", Toast.LENGTH_SHORT).show();
				
				new Thread() {
					public void run() {
						AVTransportService avTransportService = mMediaRenderer.getAVTransportService();
						while (mStarted) {
			            	try {
			            		Thread.sleep(750);
			            	} catch (Exception e) {}
			            	avTransportService.fireLastChange();
			            }
					};
				}.start();
			} catch (Exception e) {
				mWifiLock.release();
				
				Log.e(TAG, "Error starting UPnP service", e);
				if (e instanceof ValidationException) {
					ValidationException v = (ValidationException)e;
					for (ValidationError err : v.getErrors()) {
						Log.d(TAG, "   " + err.getMessage());
					}
				}
				
				Toast.makeText(this, "Failed to start rendering service.", Toast.LENGTH_SHORT).show();
				stopSelf();
			}
		}
		
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (mStarted) {
			mStarted = false;
			mWifiLock.release();
			
			Toast.makeText(this, "Stopped rendering service.", Toast.LENGTH_SHORT).show();
		}
	}

	public static void start(Context ctx) {
		Intent intent = new Intent(ctx, RenderingService.class);
		ctx.startService(intent);
	}
	
	public static void stop(Context ctx) {
		Intent intent = new Intent(ctx, RenderingService.class);
		ctx.stopService(intent);
	}
	
	private void initializeMediaPlayer() {
		mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				Toast.makeText(RenderingService.this, "Starting playback", Toast.LENGTH_SHORT).show();
				mp.start();
			}
		});
		
		mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer arg0) {
				try {
					mMediaRenderer.getAVTransportService().next(PLAYER_INSTANCE_ID);
				} catch (AVTransportException e) {
					Log.e(TAG, "Error transitioning AVTransport state machine", e);
				}
			}
		});
		
		
	}
}
