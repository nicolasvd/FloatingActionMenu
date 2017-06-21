# FloatingActionMenu
Floating Action Menu Android library built around the design FABs

```
base_src = reference to drawable used on main FAB (default is + sign)
base_background = color to use on main FAB (default = colorAccent)
base_ripple = color to use as ripple on main FAB (default = #66ffffff)
base_marginEnd = margin to use on the end of the entire menu(default = 8dp)
base_marginBottom = margin to use on the bottom of the menu(default = 8dp)
overlay_color = color used on the overlay displayed when the menu is open (default = #7F2a3441)
item_spacing = spacing between each item in the menu (default = 4dp)
enable_labels = show or not the labels (default is true)
overlay_duration = duration the overlay ripple takes to run to completion (default = 200)
label_background = drawable id of the background for the labels (default = white)
label_fontSize = font size you want to use for your labels (default = 12sp)
label_fontColor = font color you want to use for your labels (default = black)
label_marginEnd = space between the end of the label and the action button it belongs to
actions_duration = duration of the actions opening (default = 200)
```

```xml
    <be.ibad.FloatingActionMenu
        android:id="@+id/action_menu"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:base_src="@drawable/ic_mood_white_24dp"
        app:enable_labels="true"
        app:item_spacing="8dp">

        <android.support.design.widget.FloatingActionButton
            android:id="@+id/happy_item"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:contentDescription="Mood Happy"
            android:src="@drawable/ic_mood_white_24dp"
            app:backgroundTint="@color/colorPrimary"
            app:fabSize="mini" />

        <android.support.design.widget.FloatingActionButton
            android:id="@+id/bad_item"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:contentDescription="Mood Bad"
            android:src="@drawable/ic_mood_bad_white_24dp"
            app:backgroundTint="@color/colorPrimary"
            app:fabSize="mini" />

    </be.ibad.FloatingActionMenu>
...
