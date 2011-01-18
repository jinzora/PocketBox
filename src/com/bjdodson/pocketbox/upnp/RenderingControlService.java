package com.bjdodson.pocketbox.upnp;

import org.teleal.cling.model.types.UnsignedIntegerFourBytes;
import org.teleal.cling.model.types.UnsignedIntegerTwoBytes;
import org.teleal.cling.support.renderingcontrol.AbstractAudioRenderingControl;
import org.teleal.cling.support.renderingcontrol.RenderingControlException;

public class RenderingControlService extends AbstractAudioRenderingControl {

	@Override
	public boolean getMute(UnsignedIntegerFourBytes instanceId,
			String channelName) throws RenderingControlException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public UnsignedIntegerTwoBytes getVolume(
			UnsignedIntegerFourBytes instanceId, String channelName)
			throws RenderingControlException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setMute(UnsignedIntegerFourBytes instanceId,
			String channelName, boolean desiredMute)
			throws RenderingControlException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setVolume(UnsignedIntegerFourBytes instanceId,
			String channelName, UnsignedIntegerTwoBytes desiredVolume)
			throws RenderingControlException {
		// TODO Auto-generated method stub
		
	}

}
