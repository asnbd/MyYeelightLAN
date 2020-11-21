package me.asswad.myyeelightlan;

import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.N)
public class LightToggleTileService extends TileService {

    private String TAG = "LightToggleTileService";

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onStartListening() {
        Tile tile = getQsTile();
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_light_bulb_toggle));
        tile.setLabel("Light On/Off");
        tile.setContentDescription("Turn On/Off Yeelight");
        tile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();

        new LightControl(getApplicationContext()).toggle();
    }

}
