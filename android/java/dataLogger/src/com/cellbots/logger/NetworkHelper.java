
package com.cellbots.logger;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.provider.Settings;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NetworkHelper {
    private static final Logger logger = Logger.getLogger(NetworkHelper.class.getCanonicalName());

    public static BroadcastReceiver sReceiver = new BroadcastReceiver() {
            @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int state = intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                logger.info("Wifi State changed: " + state);

                if (state == WifiManager.WIFI_STATE_ENABLED) {
                    configureNetwork(context);
                    context.unregisterReceiver(this);
                }
            }
        }
    };

    public static void startConfiguration(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        context.registerReceiver(sReceiver, filter);
    }

    public static boolean configureNetwork(Context context) {
        logger.finest("Attempting to auto-configure network");

        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (manager == null)
            return false;

        Resources res = context.getResources();
        WifiConfiguration config = new WifiConfiguration();

        try {
            boolean useDhcp = res.getBoolean(R.bool.network_use_dhcp);
            ContentResolver cr = context.getContentResolver();
            if (!useDhcp) {
                Settings.System.putString(cr, Settings.System.WIFI_STATIC_IP,
                        res.getString(R.string.network_static_ip));
                Settings.System.putString(cr, Settings.System.WIFI_STATIC_NETMASK,
                        res.getString(R.string.network_static_netmask));
                Settings.System.putString(cr, Settings.System.WIFI_STATIC_GATEWAY,
                        res.getString(R.string.network_static_gateway));
                Settings.System.putString(cr, Settings.System.WIFI_STATIC_DNS1,
                        res.getString(R.string.network_static_dns_1));
            }
            Settings.System.putInt(cr, Settings.System.WIFI_USE_STATIC_IP, useDhcp ? 0 : 1);

            config.SSID = res.getString(R.string.network_wifi_essid);
            config.allowedAuthAlgorithms.clear();
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.clear();
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.clear();
            // Added for WPA2
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
            config.allowedPairwiseCiphers.clear();
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.LEAP);
            config.allowedProtocols.clear();
            config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);

            config.preSharedKey = res.getString(R.string.network_wifi_wpa_Key);
            config.status = WifiConfiguration.Status.ENABLED;

            if (!res.getString(R.string.network_wifi_bssid).equals("auto")) {
                config.BSSID = res.getString(R.string.network_wifi_bssid);
            }
        } catch (Resources.NotFoundException e) {
            logger.log(Level.SEVERE, "A resource was not found. This should never happen", e);
            return false;
        }

        // Remove all the other networks. (Go Highlander on this thing... THERE
        // CAN BE ONLY ONE)
        List<WifiConfiguration> networks = manager.getConfiguredNetworks();
        for (WifiConfiguration network : networks) {
            if (!manager.removeNetwork(network.networkId)) {
                logger.warning("Woops, can't remove network: " + network.SSID);
            }
        }

        int networkId = manager.addNetwork(config);
        if (networkId == -1) {
            return false;
        }
        manager.enableNetwork(networkId, true); /*
                                                 * Technically we are the only
                                                 * network
                                                 */
        manager.saveConfiguration();

        return true;
    }
}
