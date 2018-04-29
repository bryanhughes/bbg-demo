package com.spacetimeinsight.bbgdemo.map;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.spacetimeinsight.bbgdemo.BBGDemoApplication;
import com.spacetimeinsight.bbgdemo.R;
import com.spacetimeinsight.nucleus.android.NucleusService;
import com.spacetimeinsight.nucleuslib.Channel;
import com.spacetimeinsight.nucleuslib.datamapped.Member;
import com.spacetimeinsight.nucleuslib.datamapped.NucleusLocation;
import com.spacetimeinsight.nucleuslib.listeners.NucleusMemberListener;
import com.spacetimeinsight.nucleuslib.types.ListenerType;
import com.spacetimeinsight.nucleuslib.types.MemberStateType;

import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
                                                              GoogleMap.OnMarkerClickListener,
                                                              GoogleMap.OnCameraIdleListener {
    private static final String LOG_TAG = MapsActivity.class.getName();

    private GoogleMap map;
    private Map<String, Marker> memberMarkers = new HashMap<>();

    private NucleusMemberListener memberListener = new NucleusMemberListener() {
        @Override
        public void handleBoot(String channelRef, Member member) {
            // The member has been booted from the channel, so remove them from the map. They are still formally
            // a member of the channel. Since we are only using a single channel, we can just ignore the channelRef
            Marker marker = memberMarkers.get(member.getDeviceID());
            if ( marker != null ) {
                marker.remove();
            }
        }

        @Override
        public void onProfileChange(Member member) {
            // If the members profile image is used, then re-render the marker
        }

        @Override
        public void onStatusChange(Member member) {
            // Render a different marker to reflect the status of the member in the channel.
        }

        @Override
        public void onPresenceChange(Member member) {
            // Render a different marker to reflect the presence of the member in the channel. If they left the channel,
            // then remove them from the map
            if ( member.getState().equals(MemberStateType.LEFT) ) {
                Marker marker = memberMarkers.get(member.getDeviceID());
                if ( marker != null ) {
                    marker.remove();
                }
            }
        }

        @Override
        public void onLocationChange(Member member, NucleusLocation nucleusLocation) {
            Marker marker = memberMarkers.get(member.getDeviceID());
            if ( marker != null ) {
                marker.remove();
            }
            marker = makeMemberMarker(member);
            memberMarkers.put(member.getDeviceID(), marker);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        NucleusService nucleusService = BBGDemoApplication.getNucleusService();
        nucleusService.addListener(ListenerType.MEMBERS, memberListener);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NucleusService nucleusService = BBGDemoApplication.getNucleusService();
        nucleusService.removeListener(ListenerType.MEMBERS, memberListener);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
        if ( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            // TODO: Consider calling ActivityCompat#requestPermissions here to request the missing permissions,
            // and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        map.getUiSettings().setZoomControlsEnabled(true);
        map.setMyLocationEnabled(true);
        NucleusService nucleusService = BBGDemoApplication.getNucleusService();
        NucleusLocation location = nucleusService.getClientDevice().getCurrentLocation();
        if ( location == null ) {
            Toast.makeText(MapsActivity.this, "Location service is not available.", Toast.LENGTH_LONG).show();
        }

        MapsInitializer.initialize(this);
        map.setOnMarkerClickListener(MapsActivity.this);
        map.setOnCameraIdleListener(MapsActivity.this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if ( map != null ) {
            showMembers();
        }
    }

    private Marker makeMemberMarker(Member member) {
        Log.d(LOG_TAG, "makeMemberMarker()");
        if (map == null) {
            return null;
        }

        LatLng latLng = new LatLng(member.getCurrentDatapoint().getLocation().getLatitude(),
                                   member.getCurrentDatapoint().getLocation().getLongitude());
        Bitmap bitmap = getMarkerBitmapFromView(R.drawable.blue_pin_100);
        return map.addMarker(new MarkerOptions().position(latLng)
                                                .title(member.getScreenName())
                                                .icon(BitmapDescriptorFactory.fromBitmap(bitmap)));
    }

    private Bitmap getMarkerBitmapFromView(@DrawableRes int resId) {
        View customMarkerView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.custom_marker_layout, null);
        ImageView markerImageView = (ImageView) customMarkerView.findViewById(R.id.profile_image);
        markerImageView.setImageResource(resId);
        customMarkerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        customMarkerView.layout(0, 0, customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight());
        customMarkerView.buildDrawingCache();
        Bitmap returnedBitmap = Bitmap.createBitmap(customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight(),
                                                    Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        canvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC_IN);
        Drawable drawable = customMarkerView.getBackground();
        if (drawable != null) {
            drawable.draw(canvas);
        }
        customMarkerView.draw(canvas);
        return returnedBitmap;
    }

    @Override
    public void onCameraIdle() {
        showMembers();
    }

    private void showMembers() {
        map.clear();
        // Draw all our members on the map
        NucleusService nucleusService = BBGDemoApplication.getNucleusService();
        Channel channel = nucleusService.getCurrentChannel();
        Map<String, Member> memberMap = channel.getMembers();
        for ( Member member : memberMap.values() ) {
            // We do not want to show ourselves or the system member
            if ( member.isSystem() || member.isUser() ) {
                continue;
            }
            Marker marker = makeMemberMarker(member);
            if ( marker != null ) {
                marker.setTag(member);
                memberMarkers.put(member.getDeviceID(), marker);
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }
}
