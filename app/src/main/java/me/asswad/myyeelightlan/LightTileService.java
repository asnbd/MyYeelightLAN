package me.asswad.myyeelightlan;

import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.N)
public class LightTileService extends TileService {
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onStartListening() {
        Tile tile = getQsTile();
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_launcher_foreground));
        tile.setLabel("On");
        tile.setContentDescription("Test");
        tile.setState(Tile.STATE_ACTIVE);
        tile.updateTile();
    }

}
