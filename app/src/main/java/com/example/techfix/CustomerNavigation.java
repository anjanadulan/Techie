package com.example.techfix;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public final class CustomerNavigation {
  public static final int HOME = 0;
  public static final int SERVICES = 1;
  public static final int BOOKINGS = 2;
  public static final int PROFILE = 3;

  private CustomerNavigation() {}

  public static void bind(AppCompatActivity activity, int selected) {
    int[] containers = {R.id.navHome, R.id.navServices, R.id.navBookings, R.id.navProfile};
    int[] icons = {
        R.id.navHomeIcon, R.id.navServicesIcon, R.id.navBookingsIcon, R.id.navProfileIcon};
    int[] labels = {
        R.id.navHomeLabel, R.id.navServicesLabel, R.id.navBookingsLabel, R.id.navProfileLabel};
    Class<?>[] screens = {CustomerHomeActivity.class, ServicesActivity.class,
        RepairHistoryActivity.class, AccountActivity.class};
    int dark = ContextCompat.getColor(activity, R.color.customer_text);

    for (int index = 0; index < containers.length; index++) {
      View item = activity.findViewById(containers[index]);
      ImageView icon = activity.findViewById(icons[index]);
      TextView label = activity.findViewById(labels[index]);
      boolean isSelected = index == selected;
      item.setBackgroundResource(
          isSelected ? R.drawable.bg_customer_nav_selected : android.R.color.transparent);
      icon.setColorFilter(isSelected ? Color.WHITE : dark);
      label.setTextColor(isSelected ? Color.WHITE : dark);
      final int destination = index;
      item.setOnClickListener(view -> {
        if (destination == selected)
          return;
        Intent intent = new Intent(activity, screens[destination]);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
      });
    }
  }

  public static void open(Activity activity, Class<?> destination) {
    activity.startActivity(new Intent(activity, destination));
  }
}
