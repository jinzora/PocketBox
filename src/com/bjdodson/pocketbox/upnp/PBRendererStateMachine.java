package com.bjdodson.pocketbox.upnp;

import org.teleal.cling.support.avtransport.impl.AVTransportStateMachine;
import org.teleal.common.statemachine.States;

import com.bjdodson.pocketbox.upnp.statemachine.*;

@States({
        PBNoMediaPresent.class,
        PBStopped.class,
        PBPlaying.class,
        PBPaused.class
})
interface PBRendererStateMachine extends AVTransportStateMachine {}