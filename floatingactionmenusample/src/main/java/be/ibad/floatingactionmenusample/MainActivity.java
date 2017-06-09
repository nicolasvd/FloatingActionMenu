package be.ibad.floatingactionmenusample;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;

import be.ibad.FloatingActionMenu;
import be.ibad.listener.OnMenuItemClickListener;
import be.ibad.listener.OnMenuToggleListener;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final FloatingActionMenu actionMenu = (FloatingActionMenu) findViewById(R.id.action_menu);
        actionMenu.addOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public void onMenuItemClick(FloatingActionMenu floatingActionMenu, int index, FloatingActionButton item) {
                Snackbar.make(findViewById(R.id.coordinator), item.getContentDescription() + " " + index, Snackbar.LENGTH_SHORT).show();
                switch (item.getId()) {
                    case R.id.bad_item:
                        actionMenu.setOverlayColorRes(R.color.transparentColorPrimary);
                        actionMenu.setMenuButtonColorRes(R.color.colorPrimary);
                        actionMenu.setMenuRippleColorRes(R.color.colorAccent);
                        actionMenu.setMenuButtonRes(R.drawable.ic_mood_bad_white_24dp);
                        actionMenu.setLabelTextColorRes(R.color.colorAccent);
                        break;
                    case R.id.happy_item:
                        actionMenu.setMenuButtonColorRes(R.color.colorAccent);
                        actionMenu.setOverlayColorRes(R.color.transparentColorAccent);
                        actionMenu.setMenuRippleColorRes(R.color.colorPrimary);
                        actionMenu.setMenuButtonRes(R.drawable.ic_mood_white_24dp);
                        actionMenu.setLabelTextColorRes(R.color.colorPrimary);
                        break;
                }
            }
        });
        actionMenu.addOnMenuToggleListener(new OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean opened) {

            }
        });
    }
}
