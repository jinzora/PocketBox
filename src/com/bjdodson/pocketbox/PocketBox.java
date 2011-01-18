package com.bjdodson.pocketbox;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public class PocketBox extends Activity {
    private boolean mIsBound = false;
    private Messenger mService;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
    
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
	        if (mService != null) {
	            /*try {
	                Message msg = Message.obtain(null,
	                        BlueService.MSG_UNREGISTER_CLIENT);
	                msg.replyTo = mMessenger;
	                mService.send(msg);
	            } catch (RemoteException e) {
	                // There is nothing special we need to do if the service
	                // has crashed.
	            }*/
	        }

	        // Detach our existing connection.
	        unbindService(mServiceConnection);
	        mIsBound = false;
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
	    	mService = new Messenger(service);

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
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        mService = null;
	    }
	};
}