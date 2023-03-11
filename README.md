# Tibber Pulse Reader

This tool reads out data from a Tibber Pulse locally (without roundtrip trough the cloud).

This is an early version with poor documentation and a lot work-in-progress.

## Configuration

* configure via environment variables (ideal to run in docker)
* configure via `application.yaml` config file
* see `application.yaml.example` for examples of config variables

## Running  (docker)
docker run -it --rm \
  -e "TIBBER_PULSE_SOURCE=http" \
  [...]
  ghcr.io/micw/tibber-pulse-reader:master
  
# Running (native)

Pre-built jars can be downloaded from https://mega.nz/folder/F6x0WKjB#AIfMjKHa5gU_aWJEyhrP3w . To run it, you need a Java Runtime Environment (JRE) with version 11 or higher installed. Config can be passed as environment variables or by creating appliucation.yaml in the working directory (e.g. next to the downloaded jar file).

Example:

echo "TIBBER_PULSE_SOURCE: http" > application.yaml
echo "TIBBER_PULSE_HOST: tibberbridge.localnet" >> application.yaml
[...]
java -Xmx25M -jar tibber-pulse-reader.master.jar 

Memory assignment of the process can be tuned by the -Xmx option - adjust it to your needs so that the process does not get an out of memory error.

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
