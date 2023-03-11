package de.wyraz.tibberpulse.sink;

import java.io.IOException;

import de.wyraz.tibberpulse.sml.SMLMeterData;

public interface IMeterDataPublisher {
	public void publish(SMLMeterData data) throws IOException;
}
