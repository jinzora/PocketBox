package com.bjdodson.pocketbox.upnp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.teleal.cling.UpnpService;
import org.teleal.cling.UpnpServiceImpl;
import org.teleal.cling.binding.LocalServiceBindingException;
import org.teleal.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.teleal.cling.model.DefaultServiceManager;
import org.teleal.cling.model.ValidationException;
import org.teleal.cling.model.meta.DeviceDetails;
import org.teleal.cling.model.meta.DeviceIdentity;
import org.teleal.cling.model.meta.Icon;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.meta.ManufacturerDetails;
import org.teleal.cling.model.meta.ModelDetails;
import org.teleal.cling.model.types.DLNACaps;
import org.teleal.cling.model.types.DLNADoc;
import org.teleal.cling.model.types.DeviceType;
import org.teleal.cling.model.types.UDADeviceType;
import org.teleal.cling.model.types.UDN;
import org.teleal.cling.model.types.UnsignedIntegerFourBytes;
import org.teleal.cling.support.avtransport.impl.AVTransportService;
import org.teleal.cling.support.connectionmanager.ConnectionManagerService;
import org.teleal.cling.support.model.AVTransport;

import android.media.MediaPlayer;

import com.bjdodson.pocketbox.upnp.statemachine.PBNoMediaPresent;

/**
 * Provides UPnP device details for the MediaRenderer.
 * This class can be run directly or may be used
 * to return service details to embed in another
 * UPnP service instance.
 *
 */
public class MediaRenderer implements Runnable {
	public static final String LOG_NAME = "com.bjdodson.pocketbox";
	private static Logger JLog = Logger.getLogger(LOG_NAME);
	private static MediaRenderer sMediaRenderer;
	private static final UnsignedIntegerFourBytes sInstanceId = new UnsignedIntegerFourBytes(0);
	
	private MediaPlayer mediaPlayer;
	private AVTransportService avTransportService = null;
	private AVTransport avTransport = null;
	private PlaylistManagerService playlistManagerService = null;
	
	public static void main(String[] args) throws Exception {
        // Start a user thread that runs the UPnP stack
		JLog.info("Starting service");
		
        Thread serverThread = new Thread(getInstance());
        serverThread.setDaemon(false);
        serverThread.start();
    }
	
	private MediaRenderer() {
		if (sMediaRenderer == null) {
			sMediaRenderer = this;
		}
	}
	
	public static UnsignedIntegerFourBytes getPlayerInstanceId() {
		return sInstanceId;
	}
	
	public PlaylistManagerService getPlaylistManager() {
		return playlistManagerService;
	}
	
	public AVTransportService getAVTransportService() {
		return avTransportService;
	}
	
	public MediaPlayer getMediaPlayer() {
		return mediaPlayer;
	}
	
	public void setMediaPlayer(MediaPlayer mediaPlayer) {
		this.mediaPlayer = mediaPlayer;
	}
	
	public void setAVTransport(AVTransport transport) {
		this.avTransport = transport;
	}
	
	public AVTransport getAVTransport() {
		return this.avTransport;
	}
	
	public static MediaRenderer getInstance() {
		if (sMediaRenderer == null) {
			sMediaRenderer = new MediaRenderer();
		}
		return sMediaRenderer;
	}
	
	public void run() {
        try {

            final UpnpService upnpService = new UpnpServiceImpl();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    upnpService.shutdown();
                }
            });

            // Add the bound local device to the registry
            upnpService.getRegistry().addDevice(createDevice());
            
            /*
            while (true) {
            	try {
            		Thread.sleep(750);
            	} catch (Exception e) {}
            	avTransportService.fireLastChange();
            }
            */

        } catch (Exception ex) {
            System.err.println("Exception occured: " + ex);
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }
	
	protected LocalDevice createDevice() throws ValidationException,
			LocalServiceBindingException, IOException {

		DeviceType type = new UDADeviceType("MediaRenderer", 1);
		DeviceIdentity identity = new DeviceIdentity(UDN.uniqueSystemIdentifier("PocketBox Renderer v1"));
		DeviceDetails details = getDeviceDetails();

		URI uri = null;
		try {
			uri = new URI("assets/icon.png");
		} catch (Exception e) {}
		InputStream iconStream = new FileInputStream("assets/icon.png");
		Icon icon = new Icon("image/png", 48, 48, 8, uri, iconStream);

		return new LocalDevice(identity, type, details, icon, getServices());
	}

	private static LocalService[] mServices = null;
	/**
	 * Defines the services associated with our MediaRenderer
	 * UPnP device.
	 */
	@SuppressWarnings("unchecked")
	public static LocalService[] getServices() {
		if (mServices != null) {
			return mServices;
		}
		
		List<LocalService> localServices = new ArrayList<LocalService>();
		
		/** ConnectionManager **/
		LocalService<ConnectionManagerService> connectionManagerService =
	        new AnnotationLocalServiceBinder().read(ConnectionManagerService.class);
		connectionManagerService.setManager(
		        new DefaultServiceManager<ConnectionManagerService>(
		        		connectionManagerService,
		                ConnectionManagerService.class
		        )
		);
		localServices.add(connectionManagerService);

		/** RenderingControl **/
		LocalService<RenderingControlService> renderingControlService =
	        new AnnotationLocalServiceBinder().read(RenderingControlService.class);
		renderingControlService.setManager(
		        new DefaultServiceManager<RenderingControlService>(
		        		renderingControlService,
		        		RenderingControlService.class
		        )
		);
		localServices.add(renderingControlService);

		/** AVTransport **/
		LocalService<AVTransportService> avTransportService =
	        new AnnotationLocalServiceBinder().read(AVTransportService.class);
		final AVTransportService avTransportImpl = new AVTransportService(
                PBRendererStateMachine.class,   // All states
                PBNoMediaPresent.class  // Initial state
        );
		avTransportService.setManager(
	        new DefaultServiceManager<AVTransportService>(avTransportService, null) {
	            @Override
	            protected AVTransportService createServiceInstance() throws Exception {
	                return avTransportImpl;
	            }
	        }
		);
		localServices.add(avTransportService);
		MediaRenderer.getInstance().avTransportService = avTransportImpl;
		
		/** PlaylistManager **/
		LocalService<PlaylistManagerService> playlistManagerService =
	        new AnnotationLocalServiceBinder().read(PlaylistManagerService.class);
		final PlaylistManagerService playlistManagerImpl = new PlaylistManagerService(MediaRenderer.getInstance());
		playlistManagerService.setManager(
		        new DefaultServiceManager<PlaylistManagerService>(playlistManagerService, null) {
		        	@Override
		        	protected PlaylistManagerService createServiceInstance()
		        			throws Exception {
		        		return playlistManagerImpl;
		        	}
		        }
		);
		localServices.add(playlistManagerService);
		MediaRenderer.getInstance().playlistManagerService = playlistManagerImpl;
		
		
		mServices = localServices.toArray(new LocalService[] {});
		return mServices;
	}
	
	public DeviceDetails getDeviceDetails() {
		return new DeviceDetails("PocketBox",
				new ManufacturerDetails("PocketBox"), 
				new ModelDetails("PocketBox", "A media renderer for Android","v1"),
				new DLNADoc[]{
                    new DLNADoc("DMR", DLNADoc.Version.V1_5)
                },
                new DLNACaps(new String[] {
            		//"av-upload", "image-upload", "audio-upload"
                }));
	}

}
