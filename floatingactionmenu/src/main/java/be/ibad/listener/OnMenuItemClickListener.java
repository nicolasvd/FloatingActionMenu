package be.ibad.listener;

import android.support.design.widget.FloatingActionButton;

import be.ibad.FloatingActionMenu;

public interface OnMenuItemClickListener {
    void onMenuItemClick(FloatingActionMenu floatingActionMenu, int index, FloatingActionButton item);
}