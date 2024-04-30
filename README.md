# viam-numato-relay-usb-android
Viam modular component for Numato USB relays on Android devices

# viam-numato-relay-usb-android

This module implements the [Viam generic component API](https://docs.viam.com/components/generic/) in an mcvella:generic:numato-relay-usb-android model.
With this component, you can control Numato USB devices in your Viam Android projects.

## Requirements

Viam server must be running on an Android device that has [OTG capabilities](https://en.wikipedia.org/wiki/USB_On-The-Go), with a [Numato](https://numato.com/product-category/automation/relay-modules/usb-relay/) USB serial relay device connected.

Note that currently, the expectation is that this is the only USB serial device connected; it will assume the first serial device is a Numato relay.

## Build and Run

To use this module, follow these instructions to [add a module from the Viam Registry](https://docs.viam.com/registry/configure/#add-a-modular-resource-from-the-viam-registry) and select the `mcvella:generic:numato-relay-usb-android` model from the [`mcvella:generic:numato-relay-usb-android` module](https://app.viam.com/module/mcvella/mcvella:generic:numato-relay-usb-android).

## Configure

> [!NOTE]  
> Before configuring your mqtt-grpc service, you must [create a machine](https://docs.viam.com/manage/fleet/machines/#add-a-new-machine).

Navigate to the **Config** tab of your robot’s page in [the Viam app](https://app.viam.com/).
Click on the **Components** subtab and click **Create**.
Select the `generic` type, then select the `mcvella:generic:numato-relay-usb-android` model.
Enter a name for your component and click **Create**.

On the new component panel, copy and paste the following attribute template into your pubsub’s **Attributes** box:

```json
{
}
```

> [!NOTE]  
> For more information, see [Configure a Robot](https://docs.viam.com/manage/configuration/).

### Attributes

The following attributes are available for `mcvella:generic:numato-relay-usb-android`:

| Name | Type | Inclusion | Description |
| ---- | ---- | --------- | ----------- |
| |  |  |  |

### Example Configurations

A typical configuration might look like:

```json
{
}
```

## API Usage

DoCommand() can be used to turn a relay on, turn a relay off, or read a relay's status.

Example:

``` python
# turn relay 0 on
await relay.do_command({'on': '0'})
# read the status of relay 1
status = await relay.do_command({'read': "1"})
```

### on

If the key *on* is passed to DoCommand(), the a string value representing the index of a relay (starting with 0 for the first relay) is passed as the value.
This will turn the specified relay on.

### off

If the key *off* is passed to DoCommand(), the a string value representing the index of a relay (starting with 0 for the first relay) is passed as the value.
This will turn the specified relay off.

### read

If the key *read* is passed to DoCommand(), the a string value representing the index of a relay (starting with 0 for the first relay) is passed as the value.
This will read and return the status for the specified relay, either "on" or "off".

## TODO
