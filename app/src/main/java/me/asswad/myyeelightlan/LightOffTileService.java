package me.asswad.myyeelightlan;

import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.N)
public class LightOffTileService extends TileService {

    private String TAG = "LightOffTileService";

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onStartListening() {
        Tile tile = getQsTile();
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_light_bulb_off));
        tile.setLabel("Light Off");
        tile.setContentDescription("Turn Off Yeelight");
        tile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        Log.d(TAG, "onClick: Clicked Light Off Tile");
        new LightControl(getApplicationContext()).turnOff();
    }

}
