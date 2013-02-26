
package com.cellbots.logger.localServer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.cellbots.logger.R;

/**
 * Control activity for starting/stopping the local logger HTTP server. Devices
 * on the same network as the logger should be able to see a real time snapshot
 * of the sensor values by accessing one of the listed IP addresses.
 * 
 * @author clchen@google.com (Charles L. Chen)
 */
public class ServerControlActivity extends Activity {
    private TextView ipAddressesView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.server_control);

        ipAddressesView = (TextView) findViewById(R.id.serverIpAddressesTextView);

        Button startButton = (Button) findViewById(R.id.serverStartButton);
        startButton.setOnClickListener(new OnClickListener() {
                @Override
            public void onClick(View v) {
                Intent i = new Intent(ServerControlActivity.this, LoggingService.class);
                i.putExtra(LoggingService.EXTRA_COMMAND, LoggingService.EXTRA_COMMAND_START);
                startService(i);
                ipAddressesView.setText(
                        "Server Address:\n" + LocalHttpServer.getLocalIpAddresses());
            }
        });

        Button stopButton = (Button) findViewById(R.id.serverStopButton);
        stopButton.setOnClickListener(new OnClickListener() {
                @Override
            public void onClick(View v) {
                Intent i = new Intent(ServerControlActivity.this, LoggingService.class);
                i.putExtra(LoggingService.EXTRA_COMMAND, LoggingService.EXTRA_COMMAND_STOP);
                startService(i);
                ipAddressesView.setText("Server Address:\nN/A");
            }
        });
    }

}
