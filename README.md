# Tibber Pulse Reader

This tool reads out data from a Tibber Pulse locally (without roundtrip trough the cloud).

This is an early version with poor documentation and a lot work-in-progress.

## Configuration

* configure via environment variables (ideal to run in docker)
* configure via `application.yaml` config file
* see `application.yaml.example` for examples of config variables

## Running  (docker)

```
docker run -it --rm \
  -e "TIBBER_PULSE_SOURCE=http" \
  [...]
  ghcr.io/micw/tibber-pulse-reader:master

```

* pass all config parameters as environment variables
* alternatively, create an `application.yaml` and mount it to the docker container at `/application.yaml`


To fetch the latest docker image, run:

```
docker pull ghcr.io/micw/tibber-pulse-reader:master
```
  
# Running (native)

> **⚠ jars are not yet available for download**

Pre-built jars can be downloaded from https://mega.nz/folder/F6x0WKjB#AIfMjKHa5gU_aWJEyhrP3w . To run it, you need a Java Runtime Environment (JRE) with version 11 or higher installed. Config can be passed as environment variables or by creating appliucation.yaml in the working directory (e.g. next to the downloaded jar file).

Example:

echo "TIBBER_PULSE_SOURCE: http" > application.yaml
echo "TIBBER_PULSE_HOST: tibberbridge.localnet" >> application.yaml
[...]
java -Xmx25M -jar tibber-pulse-reader.master.jar 

Memory assignment of the process can be tuned by the -Xmx option - adjust it to your needs so that the process does not get an out of memory error.

## Modes of access

### HTTP access

This access method allows to read data from an almost unmodified Tibber Pulse Bridge. Only the web interface must be enabled. A description how this can be done is in my blog article https://blog.wyraz.de/allgemein/a-brief-analysis-of-the-tibber-pulse-bridge/ .

Configuration parameters:

* `TIBBER_PULSE_SOURCE=http` (required) - enable the HTTP based access to Tibber Pulse Bridge
* `TIBBER_PULSE_HOST` (required) - Hostname or IP address of the Tibber Pulse Bridge
* `TIBBER_PULSE_NODE_ID` (default 1) - ID of the connected node (Tibber Pulse IR) - only required if more than one node is connected to the Tibber Pulse Bridge
* `TIBBER_PULSE_USERNAME` (optional) - Username to access the Tibber Pulse Bridge (usually `admin`)
* `TIBBER_PULSE_PASSWORD` (optional) - Password to access the Tibber Pulse Bridge (the initial password is printed on the QR-code of your Tibber Pulse Gateway)
* `TIBBER_PULSE_CRON` (default `*/15 * * * * *`) - Cron expression how often the data should be read from Tibber Pulse Bridge. The default is every 15 seconds

### MQTT access

To access the MQTT data, the Tibber Pulse Bridge must be modified to talk to a local MQTT server. The local MQTT server must be configured to bridge to tibber's MQTT server. The process is described in https://github.com/MSkjel/LocalPulse2Tibber .

Configuration parameters:

* `TIBBER_PULSE_SOURCE=mqtt` (required) - enable the MQTT based access to Tibber Pulse Bridge
* `MQTT_SOURCE_HOST` (required) - Hostname or IP address of the MQTT server to subscribe to
* `MQTT_SOURCE_TLS` (default `false`) - set to true to use TLS/SSL for MQTT connection
* `MQTT_SOURCE_PORT` (default `1883`) - Port of the MQTT server
* `MQTT_SOURCE_USERNAME` (optional) - username for authentification if required by the MQTT server
* `MQTT_SOURCE_PASSWORD` (optional) - password for authentification if required by the MQTT server
* `MQTT_SOURCE_TOPIC` (required) - The topic to subscribe to and read SML from
* `MQTT_SOURCE_PAYLOAD_ENCODING` (default `HEX`) - Encoding of the payload. Supported values: `HEX`, `BINARY`

## Publishers

Publishers sends the decoded data to other systems like databases or message brokers. The followng publishers are available.

### MQTT publisher

Publishes meter readings via MQTT. Each value is published on a separate topic. The topic and payload can be configured using placeholders.

Configuration parameters:

* `PUBLISH_MQTT_ENABLED` (required) - set to `true` to enable publishing over MQTT
* `PUBLISH_MQTT_HOST` (required) - Hostname or IP address of the MQTT server to publish to
* `PUBLISH_MQTT_TLS` (default `false`) - set to true to use TLS/SSL for MQTT connection
* `PUBLISH_MQTT_PORT` (default 1883) - Port of the MQTT server
* `PUBLISH_MQTT_USERNAME` (optional) - username for authentification if required by the MQTT server
* `PUBLISH_MQTT_PASSWORD` (optional) - password for authentification if required by the MQTT server
* `PUBLISH_MQTT_TOPIC`(default `{meterId}/{nameOrObisCode}`) - a template to build the topic to publish to. See below for allowed placeholders.
* `PUBLISH_MQTT_VALUE`(default `{value}`) - a template to build the payload to publish. See below for allowed placeholders.

#### Placeholders for topic and payload

* `{meterId}`: The id of the meter like '01EBZ0123456789' or 'unknown' if no meter ID is received
* `{obisCode}`: The OBIS code of the reading
* `{name}`: A friendly name of the OBIS code like 'energyImportTotal' or blank if no name is known
* `{nameOrObisCode}`: A friendly name of the OBIS code or the OBIS code if the name is not known
* `{value}`: The numeric value of the reading
* `{unit}`: The unit of the reading or blank if there is no unit

### OpenMetrics publisher

Publishes metrics in [OpenMetrics format](https://github.com/OpenObservability/OpenMetrics/blob/main/specification/OpenMetrics.md) via HTTP. Usefull to publish to [VictoriaMetrics](https://docs.victoriametrics.com/url-examples.html#apiv1importprometheus) or [Prometheus pushgateway](https://github.com/prometheus/pushgateway/blob/master/README.md).

Configuration parameters:

* `PUBLISH_OPENMETRICS_ENABLED` (required) - set to `true` to enable publishing over MQTT
* `PUBLISH_OPENMETRICS_URL` (required) - URL of the endpoint to publish to
* `PUBLISH_OPENMETRICS_USERNAME` (optional) - username for HTTP basic authentification
* `PUBLISH_OPENMETRICS_PASSWORD` (optional) - password for HTTP basic authentification


## Other configuration

* `LOG_LEVEL` (default `info`) - Log level to use by the application. Valid values are `debug`, `info`, `warn` and `error`.
    * run with `debug` to see all SML messages before/after decoding
* `IGNORE_SML_CRC_ERRORS` (default `false`) - If set to true, an SML message containing CRC errors will still be processed. Only usefull for testing with problematic meters.
* `PUBLISH_INTERVAL` (default empty) - Can be set to a cron expression to publish only one reading per interval.

### Filtering readings

The config parameter `PUBLISH_FILTERS` allows to specify a list of filters applied to readings. Multiple filters are separated by whitespace or newline.

A filter consists of a field selector (OBIS code or name) and a rule name (see below). Field names an rule names are not case sensitive.

Example:

Input:
```
1-0:1.8.0*255 / energyImportTotal = 123123.45 WATT_HOURS
1-0:2.8.0*255 / energyExportTotal = 345234.56 WATT_HOURS
1-0:16.7.0*255 / powerTotal = 11111.22 WATT
1-0:36.7.0*255 / powerL1 = 22333.33 WATT
```

Filters:
```
1-0:1.8.0*255=IGNORE
powerL1=IGNORE
energyExportTotal=kWh
powerL1=KILOWATT
```

Result:
```
1-0:2.8.0*255 / energyExportTotal = 345.23456 KILOWATT_HOURS
1-0:16.7.0*255 / powerTotal = 11.11122 KILOWATT
```

The following rules are allowed:

* `IGNORE` - the reading will not be published
* `KILOWATT` - if the reading is in WATT, it will be converted to KILOWATT
    * `kW` - alias for `KILOWATT`
* `KILOWATT_HOURS` - if the reading is in WATT_HOURS, it will be converted to KILOWATT_HOURS
    * `kWh` - alias for `KILOWATT_HOURS`
