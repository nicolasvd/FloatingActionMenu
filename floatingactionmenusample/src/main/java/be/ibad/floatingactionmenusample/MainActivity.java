package be.ibad.floatingactionmenusample;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import be.ibad.FloatingActionMenu;
import be.ibad.listener.OnMenuItemClickListener;
import be.ibad.listener.OnMenuToggleListener;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionMenu actionMenu = (FloatingActionMenu) findViewById(R.id.action_menu);
        actionMenu.addOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public void onMenuItemClick(FloatingActionMenu floatingActionMenu, int index, FloatingActionButton item) {
                Snackbar.make(findViewById(R.id.coordinator), item.getContentDescription(), Snackbar.LENGTH_SHORT).show();
            }
        });
        actionMenu.addOnMenuToggleListener(new OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean opened) {
                Toast.makeText(MainActivity.this, "Toggle " + opened, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
