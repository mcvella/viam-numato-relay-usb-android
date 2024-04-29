package com.mcvella.numatoRelayUsbAndroid;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.viam.common.v1.Common;
import com.viam.sdk.android.module.Module;
import android.Manifest;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;


import com.viam.sdk.core.component.generic.Generic;
import com.viam.sdk.core.resource.Model;
import com.viam.sdk.core.resource.ModelFamily;
import com.viam.sdk.core.resource.Registry;
import com.viam.sdk.core.resource.Resource;
import com.viam.sdk.core.resource.ResourceCreatorRegistration;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.logging.Logger;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import viam.app.v1.Robot;

public class Main {

  private static UsbManager usb;
  private static Context cxt;
  private static final String ACTION_USB_PERMISSION = "com.android.usb.USB_PERMISSION";
  private static final String[] PERMISSIONS = {
          Manifest.permission.ACCESS_COARSE_LOCATION,
          Manifest.permission.ACCESS_FINE_LOCATION
  };

  public static void main(final String[] args) {

    Registry.registerResourceCreator(
        Generic.SUBTYPE,
        NumatoRelay.MODEL,
        new ResourceCreatorRegistration(NumatoRelay::new, NumatoRelay::validateConfig)
    );
    final Module module = new Module(args);
    usb = (UsbManager) module.getParentContext().getSystemService(Context.USB_SERVICE);
    cxt = module.getParentContext();
    module.start();
  }

  public static class NumatoRelay extends Generic {

    public static final Model MODEL = new Model(new ModelFamily("mcvella", "generic"), "numato-relay-usb-android");
    private static final Logger LOGGER = Logger.getLogger(NumatoRelay.class.getName());
    private UsbSerialPort relay;

    public NumatoRelay(Robot.ComponentConfig config,
        Map<Common.ResourceName, Resource> dependencies) {
      super(config.getName());

      if ( relay != null && relay.isOpen()) {
        try {
          relay.close();
          relay = null;
        } catch (IOException e) {
          LOGGER.warning("Could not disconnect Numato relay device; perhaps was not already connected");
        }
      }

    }

    private boolean openRelay() {
      List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usb);
      if (availableDrivers.isEmpty()) {
        LOGGER.severe("No serial USB devices found");
        return false;
      }
      LOGGER.warning("USB devices found:" + Arrays.toString(availableDrivers.toArray()));

      UsbSerialDriver driver;
      try {
          driver = availableDrivers.get(0);
      } catch (Exception ignored) {
          LOGGER.severe("No correct Numato USB serial devices, could not connect");
          return false;
      }

      // Check and grant permissions
      if (!checkAndRequestPermission(usb, driver.getDevice())) {
        LOGGER.severe("Please try again and grant permission to connect to Numato USB serial device");
        return false;
      }

      // Open USB device
      UsbDeviceConnection connection = usb.openDevice(driver.getDevice());
      if (connection == null) {
          LOGGER.severe("Error opening Numato USB device");
          return false;
      }

      try {
        // Open first available serial port
        relay = driver.getPorts().get(0);
        relay.open(connection);
        relay.setParameters(19200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

        LOGGER.info("Successfully connected to Numato relay device");
        return true;
      } catch (IOException e) {
        LOGGER.severe("Failure connecting to detected Numato relay device");
        return false;
      }
    }
    public static Set<String> validateConfig(final Robot.ComponentConfig ignored) {
      return new HashSet<>();
    }

    private boolean checkAndRequestPermission(UsbManager manager, UsbDevice usbDevice) {
      // Check if permissions already exists
      if (hasPermissions(cxt.getApplicationContext(), PERMISSIONS)
              && manager.hasPermission(usbDevice))
          return true;
      else {
          // Request USB permission
          PendingIntent pendingIntent = PendingIntent.getBroadcast(cxt.getApplicationContext(),
                  0, new Intent(ACTION_USB_PERMISSION), 0);
          manager.requestPermission(usbDevice, pendingIntent);
          
          // this is a hack and means permissions must be accepted within 10 seconds of being asked
          try {
            Thread.sleep(10000);
          } catch (InterruptedException e) {
            return false;
          }
          return true;
      }
    }
    public boolean hasPermissions(Context context, String... permissions) {
      if (context != null && permissions != null) {
          for (String permission : permissions) {
              if (ActivityCompat.checkSelfPermission(context, permission)
                      != PackageManager.PERMISSION_GRANTED) {
                  return false;
              }
          }
      }
      return true;
    }

    @Override
    public Struct doCommand(Map<String, Value> command) {
      final Struct.Builder builder = Struct.newBuilder();

      if (relay == null || relay.getSerial() == null) {
        LOGGER.severe("Can't get Numato Serial");

        Boolean open = openRelay();
        LOGGER.severe("Numato status: " + open);

        if (!open) {
          return builder.putFields("Unable to open relay", Value.newBuilder().setStringValue("error").build()).build();
        }
      }

      String serialCommand = "";
      String type = "write";
      if (command.containsKey("on")) {
        serialCommand = "relay on " + command.get("on").getStringValue() + "\r";
      } else if (command.containsKey("off")) {
        serialCommand = "relay off " + command.get("off").getStringValue() + "\r";
      } else if (command.containsKey("read")) {
        serialCommand = "relay read " + command.get("read").getStringValue() + "\r";
        type = "read";
      } else if (command.containsKey("reset")) {
        serialCommand = "reset\r";
      }

      if (type == "write") {
        try {
          relay.write("\r".getBytes(), 100);
          relay.write(serialCommand.getBytes(), 100);
          return builder.putFields("serial command written", Value.newBuilder().setStringValue(serialCommand).build()).build();
        } catch (IOException e) {
          return builder.putFields("serial write error sending", Value.newBuilder().setStringValue(serialCommand).build()).build();
        }
      } else {
        byte[] resp = new byte[64];
        try {
          relay.write("\r".getBytes(), 100);
          relay.write(serialCommand.getBytes(), 100);
          relay.read(resp, 100);
        } catch (IOException e) {
          return builder.putFields("serial write error reading", Value.newBuilder().setStringValue(serialCommand).build()).build();
        }
        String response = new String(resp, StandardCharsets.UTF_8);
        return builder.putFields("serial command read", Value.newBuilder().setStringValue(response).build()).build();
      }
    }
  }
}

