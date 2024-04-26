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
    private UsbSerialPort port;

    public NumatoRelay(Robot.ComponentConfig config,
        Map<Common.ResourceName, Resource> dependencies) {
      super(config.getName());
      List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usb);
      if (availableDrivers.isEmpty()) {
        LOGGER.severe("No serial USB devices found, restart module after connecting a device");
        return;
      }
      LOGGER.warning("USB devices found:" + Arrays.toString(availableDrivers.toArray()));

      UsbSerialDriver driver;
      try {
          driver = availableDrivers.get(0);
      } catch (Exception ignored) {
          LOGGER.severe("No correct USB serial devices, could not connect");
          return;
      }

      // Check and grant permissions
      if (!checkAndRequestPermission(usb, driver.getDevice())) {
        LOGGER.severe("Please restart and grant permission");
        return;
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
          return false;
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
      String serialCommand = "";
      if (command.containsKey("on")) {
        serialCommand = "relay on " + command.get("on").getStringValue() + "\r";
      } else if (command.containsKey("off")) {
        serialCommand = "relay off " + command.get("off").getStringValue() + "\r";
      } else if (command.containsKey("read")) {
        serialCommand = "relay read " + command.get("read").getStringValue() + "\r";
      } else if (command.containsKey("reset")) {
        serialCommand = "reset\r";
      }
      return builder.putFields("serial command", Value.newBuilder().setStringValue(serialCommand).build()).build();
    }
  }
}

