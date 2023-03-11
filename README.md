# Tibber Pulse Reader

This tool reads out data from a Tibber Pulse locally (without roundtrip trough the cloud).

This is an early version with poor documentation and a lot work-in-progress.

## Configuration

* configure via environment variables (ideal to run in docker)
* configure via `application.yaml` config file
* see `application.yaml.example` for examples of config variables

## Modes of access

### HTTP access

This access method allows to read data from an almost unmodified Tibber Pulse Gateway. Only the web interface must be enabled. A description how this can be done is in my blog article https://blog.wyraz.de/allgemein/a-brief-analysis-of-the-tibber-pulse-bridge/ .

TODO: describe how to configure http access mode

### MQTT access

> **⚠ This method is not yet implemented**

To access the MQTT data, the Tibber Pulse Gateway must be modified to talk to a local MQTT server. The local MQTT server must be configured to bridge to tibber's MQTT server. The process is described in https://github.com/MSkjel/LocalPulse2Tibber .

## Publishers

### MQTT publisher

* publishes readings via MQTT
* Topic and payload placeholders:
    * `{meterId}`: The id of the meter like '01EBZ0123456789' or 'unknown' if no meter ID is received
    * `{obisCode}`: The OBIS code of the reading
    * `{name}`: A friendly name of the OBIS code like 'energyImportTotal' or blank if no name is known
    * `{nameOrObisCode}`: A friendly name of the OBIS code or the OBIS code if the name is not known
    * `{value}`: The numeric value of the reading
    * `{unit}`: The unit of the reading or blank if there is no unit
