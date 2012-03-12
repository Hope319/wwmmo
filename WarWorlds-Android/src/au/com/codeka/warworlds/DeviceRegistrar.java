
package au.com.codeka.warworlds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import warworlds.Warworlds.DeviceRegistration;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings.Secure;
import au.com.codeka.warworlds.api.ApiClient;

/**
 * Register/unregister with the third-party App Engine server using
 * RequestFactory.
 */
public class DeviceRegistrar {
    private static Logger log = LoggerFactory.getLogger(DeviceRegistrar.class);

    public static void register(final Context context, String deviceRegistrationID) {
        final SharedPreferences settings = Util.getSharedPreferences(context);

        String registrationKey = null;
        try {
            DeviceRegistration registration = DeviceRegistration.newBuilder()
                .setDeviceId(Secure.getString(context.getContentResolver(), Secure.ANDROID_ID))
                .setDeviceRegistrationId(deviceRegistrationID)
                .setDeviceBuild(android.os.Build.DISPLAY)
                .setDeviceManufacturer(android.os.Build.MANUFACTURER)
                .setDeviceModel(android.os.Build.MODEL)
                .setDeviceVersion(android.os.Build.VERSION.RELEASE)
                .build();

            // the post will update the key field in the protocol buffer for us
            registration = ApiClient.postProtoBuf("devices", registration,
                    DeviceRegistration.class);
            registrationKey = registration.getKey();
        } catch(Exception ex) {
            log.error("Failure registring device.", ex);
            forgetDeviceRegistration(context);
            return;
        }

        SharedPreferences.Editor editor = settings.edit();
        editor.putString("DeviceRegistrar.registrationKey", registrationKey);
        editor.commit();
    }

    public static void unregister(final Context context) {
        final SharedPreferences settings = Util.getSharedPreferences(context);
        String registrationKey = settings.getString("DeviceRegistrar.registrationKey", "");
        if (registrationKey == "") {
            log.info("No unregistration required, device not registered.");
            return;
        }

        try {
            String url = "devices/" + registrationKey;
            ApiClient.delete(url);
        } catch(Exception ex) {
            log.error("Failure unregistering device.", ex);
        }

        forgetDeviceRegistration(context);
    }

    private static void forgetDeviceRegistration(Context context) {
        final SharedPreferences settings = Util.getSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove("DeviceRegistrar.registrationKey");
        editor.commit();
    }
}
