package com.example.vahanl.onmyway;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.accountkit.Account;
import com.facebook.accountkit.AccountKit;
import com.facebook.accountkit.AccountKitCallback;
import com.facebook.accountkit.AccountKitError;
import com.facebook.accountkit.PhoneNumber;
import com.facebook.login.LoginManager;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.android.PolyUtil;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.makeramen.roundedimageview.RoundedTransformationBuilder;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class AccountActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int MY_LOCATION_REQUEST_CODE = 1;
    private static final String TAG = "AccountActivity";
    ProfileTracker profileTracker;
    ImageView profilePic;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;

    private AddressResultReceiver mResultReceiver;
    private String mAddressOutput;
    private Location mLastLocation;

    private PlaceAutocompleteFragment mAutocompleteFragmentSrc;
    private PlaceAutocompleteFragment mAutocompleteFragmentDest;

    private SupportMapFragment mMapFragment;

    private LatLng mSrcLatLng;
    private LatLng mDestLatLng;
    private GeoApiContext mGeoApiContext;

    private DirectionsResult mUserDirectionsResult;

    private DatabaseReference mFirebaseDbRef;

    private Switch mDriverFooterSwitch;

    private SeekBar mWaitSeekBar;

    private TextView mWaitSeekBarTextView;

    private User mUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        MyApplication myApplication = (MyApplication) getApplication();
        mGeoApiContext = myApplication.getGeoApiContext();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mResultReceiver = new AddressResultReceiver(new Handler());


        initializeUI();
        fetchAccountInfo();
        initializeFirebaseDb();



        intitializeMap();

        setSearchListeners();


    }

    private void intitializeMap() {
        mMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mMapFragment.getMapAsync(this);
    }

    private void initializeFirebaseDb() {
        mFirebaseDbRef = FirebaseDatabase.getInstance().getReference();


//        mFirebaseDbRef.child("routes").addChildEventListener(new ChildEventListener() {
//            @Override
//            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
//
//                User user = dataSnapshot.getValue(User.class);
//
//                Log.d(TAG, "user name: " + user.name);
//                Log.d(TAG, "user type: " + user.type);
//                Log.d(TAG, "user startTime: " + user.startTime);
//
//
//            }
//
//            @Override
//            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
//
//                User user = dataSnapshot.getValue(User.class);
//                Log.d(TAG, "onChildChanged, user name: " + user.name);
//            }
//
//            @Override
//            public void onChildRemoved(DataSnapshot dataSnapshot) {
//
//            }
//
//            @Override
//            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
//
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//
//            }
//        });
        String userType = getUserType();

        Query usersInTime = mFirebaseDbRef.child("routes").child(userType);

        mFirebaseDbRef.child("routes").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {


                for (DataSnapshot routeTypeSnapshot : dataSnapshot.getChildren()) {
                    String routType = routeTypeSnapshot.getKey();
                    if (!mUser.type.equals(routType)) {
                        for (DataSnapshot routeSnapshot : routeTypeSnapshot.getChildren()) {
                            User user = routeSnapshot.getValue(User.class);
                            if (user == null ||
                                    user.startLocation == null ||
                                    user.endLocation == null) {
                                return;
                            }
                            // meet users
                            if (user.type.equals(Constants.TYPE_DRIVER)) {
                                com.google.maps.model.LatLng origin = getLatLng(user.startLocation);
                                com.google.maps.model.LatLng dest = getLatLng(user.endLocation);
                                DirectionsResult result = null;
                                try {
                                    result = DirectionsApi.newRequest(mGeoApiContext)
                                            .origin(origin)
                                            .destination(dest)
                                            .await();
                                } catch (ApiException e) {
                                    e.printStackTrace();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                if (containsRoute(result, mUserDirectionsResult)) {
                                    String message = "I am a footer indeed:" + mUser.type +
                                            ".I am on the route of the driver: " +
                                            user.name;
                                    Log.d(TAG, message);
                                    showSnackbar(message);
                                    mMap.clear();
                                    addPolyline(result, mMap);
                                }
                            } else {
                                LatLng origin = getGmsLatLng(user.startLocation);
                                LatLng dest = getGmsLatLng(user.endLocation);
                                if (containsRoute(mUserDirectionsResult, origin, dest)) {
                                    String message = "I am driver indeed: " + mUser.type +
                                            ".I found a footer on my route named: " +
                                            user.name;
                                    Log.d(TAG, message);
                                    showSnackbar(message);
                                    mMap.clear();
                                    addMarkersToMap(origin, dest, mMap);
                                }
                            }

                        }
                    }


                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        });

    }

    private String getUserType() {
        if (mUser.type.equals(Constants.TYPE_FOOTER)) {
            return Constants.TYPE_DRIVER;
        }
        return Constants.TYPE_FOOTER;
    }

    private void addMarkersToMap(LatLng origin, LatLng dest, GoogleMap mMap) {
        BitmapDescriptor userMarker = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE);

        if (mUser.isFooter()) {

            //TODO:set custom icon
            userMarker = BitmapDescriptorFactory.defaultMarker();
        }

        mMap.addMarker(new MarkerOptions().
                position(origin)
                .icon(userMarker));
        mMap.addMarker(new MarkerOptions()
                .position(dest)
                .icon(userMarker));
    }

    private LatLng getGmsLatLng(String location) {
        double[] latlong = new double[2];
        Pattern p = Pattern.compile("(\\d+(?:\\.\\d+))");
        Matcher m = p.matcher(location);
        int i = 0;
        while (m.find()) {
            double d = Double.parseDouble(m.group(1));
            latlong[i++] = d;
        }
        return new LatLng(latlong[0], latlong[1]);
    }

    @NonNull
    private com.google.maps.model.LatLng getLatLng(String location) {

        double[] latlong = new double[2];
        Pattern p = Pattern.compile("(\\d+(?:\\.\\d+))");
        Matcher m = p.matcher(location);
        int i = 0;
        while (m.find()) {
            double d = Double.parseDouble(m.group(1));
            latlong[i++] = d;
        }

        return new com.google.maps.model.LatLng(latlong[0], latlong[1]);
    }


    private void initializeUI() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        profilePic = (ImageView) findViewById(R.id.profile_image);
        mWaitSeekBarTextView = (TextView) findViewById(R.id.waitIntervaltextView);
        mDriverFooterSwitch = (Switch) findViewById(R.id.driver_footer_switch);
        mDriverFooterSwitch.setChecked(isDriver());
        mDriverFooterSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mUser.type = Constants.TYPE_DRIVER;
                } else {
                    mUser.type = Constants.TYPE_FOOTER;
                }

                SharedPrefHelper.saveUserType(AccountActivity.this, mUser.type);
            }
        });

        mWaitSeekBar = (SeekBar) findViewById(R.id.waitSeekBar);
        mWaitSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            int progressValue = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressValue = progress;

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                mWaitSeekBarTextView.setText(String.valueOf(progressValue));
                mUser.intervalInMinutes = progressValue;
            }
        });
    }

    private boolean isDriver() {
        String userType = SharedPrefHelper.getUserType(this);
        return userType.equals(Constants.TYPE_DRIVER);
    }

    private void setSearchListeners() {
        mAutocompleteFragmentSrc = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment_src);
        mAutocompleteFragmentDest = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment_dest);
        mAutocompleteFragmentSrc.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {

                mSrcLatLng = place.getLatLng();

                Log.d(TAG, "selected place: " + place.getAddress());
            }

            @Override
            public void onError(Status status) {

                Log.e(TAG, "error: " + status.getStatusMessage());
            }
        });

        mAutocompleteFragmentDest.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                mDestLatLng = place.getLatLng();

                mUser.endLocation = mDestLatLng.toString();

                if (mSrcLatLng == null) {
                    mSrcLatLng = new LatLng(mLastLocation.getLatitude(),
                            mLastLocation.getLongitude());
                }

                mUser.startLocation = mSrcLatLng.toString();

                if (mMap != null && mSrcLatLng != null && mDestLatLng != null) {

                    String originDest = "origin: " + mSrcLatLng.toString() +
                            "dest: " + mDestLatLng.toString();

//                    mFirebaseDbRef.setValue(originDest);

                    try {
                        com.google.maps.model.LatLng origin =
                                new com.google.maps.model.LatLng(mSrcLatLng.latitude, mSrcLatLng.longitude);

                        com.google.maps.model.LatLng dest =
                                new com.google.maps.model.LatLng(mDestLatLng.latitude, mDestLatLng.longitude);

                        DirectionsResult result = DirectionsApi.newRequest(mGeoApiContext)
                                .origin(origin)
                                .destination(dest)
                                .await();

                        mUserDirectionsResult = result;
                        addPolyline(mUserDirectionsResult, mMap);
                        addMarkersToMap(result, mMap);

                    } catch (ApiException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }

            @Override
            public void onError(Status status) {

            }
        });
    }


    ///////////////Map functions///////////////////////////

    private boolean containsRoute(DirectionsResult driverResult, DirectionsResult result) {

        if (driverResult == null || result == null) {
            return false;
        }
        LatLng origin = new LatLng(result.routes[0].legs[0].startLocation.lat,
                result.routes[0].legs[0].startLocation.lng);
        LatLng dest = new LatLng(result.routes[0].legs[0].endLocation.lat,
                result.routes[0].legs[0].endLocation.lng);


        List<LatLng> decodedPath = PolyUtil.decode(driverResult.routes[0].overviewPolyline.getEncodedPath());

        return PolyUtil.isLocationOnPath(origin, decodedPath, true, 200d) &&
                PolyUtil.isLocationOnPath(dest, decodedPath, true, 200d);
    }

    private boolean containsRoute(DirectionsResult driverResult, LatLng footerOrigin, LatLng footerDest) {
        if (driverResult == null ||
                footerOrigin == null ||
                footerDest == null) {
            return false;
        }
        List<LatLng> decodedPath = PolyUtil.decode(driverResult.routes[0].overviewPolyline.getEncodedPath());

        return PolyUtil.isLocationOnPath(footerOrigin, decodedPath, true, 200d) &&
                PolyUtil.isLocationOnPath(footerDest, decodedPath, true, 200d);
    }

    private void addMarkersToMap(DirectionsResult results, GoogleMap mMap) {

        BitmapDescriptor userMarker = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE);

        if (mUser.isFooter()) {

            //TODO:set custom icon
            userMarker = BitmapDescriptorFactory.defaultMarker();
        }

        mMap.addMarker(new MarkerOptions().
                position(new LatLng(results.routes[0].legs[0].startLocation.lat,
                        results.routes[0].legs[0].startLocation.lng))
                .icon(userMarker)
                .title(results.routes[0].legs[0].startAddress));
        mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(results.routes[0].legs[0].endLocation.lat,
                        results.routes[0].legs[0].endLocation.lng)).title(results.routes[0].legs[0].startAddress)
                        .snippet(getEndLocationTitle(results))
                .icon(userMarker)
        );
    }

    private String getEndLocationTitle(DirectionsResult results) {
        return "Time :" + results.routes[0].legs[0].duration.humanReadable +
                " Distance :" + results.routes[0].legs[0].distance.humanReadable;
    }

    private void addPolyline(DirectionsResult results, GoogleMap mMap) {
        //TODO: ArrayIndexOutOfBoundsException
        List<LatLng> decodedPath = PolyUtil.decode(results.routes[0].overviewPolyline.getEncodedPath());

        int userColor = Color.BLACK;
        if (mUser.type.equals(Constants.TYPE_FOOTER)) {
            userColor = Color.RED;
        }
        mMap.addPolyline(new PolylineOptions().color(userColor).addAll(decodedPath));
    }

    private void moveCamera(Location location) {
        LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.addMarker(new MarkerOptions().position(myLocation)
                .title("Marker in my location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 14.0f));
    }

    ///////////////Map functions///////////////////////////

    @Override
    public void onStart() {
        super.onStart();

        if (!checkPermissions()) {
            requestPermissions();
        } else {
            getAddress();
        }
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                MY_LOCATION_REQUEST_CODE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // unregister the profile tracker receiver
        profileTracker.stopTracking();
    }


    ///////////////Account functions///////////////////////////

    private void fetchAccountInfo() {

        profileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {
                if (currentProfile != null) {
                    displayProfileInfo(currentProfile);
                }
            }
        };

        mUser = new User();
        mUser.type = SharedPrefHelper.getUserType(this);
        if (AccessToken.getCurrentAccessToken() != null) {
            // If there is an access token then Login Button was used
            // Check if the profile has already been fetched
            Profile currentProfile = Profile.getCurrentProfile();
            mUser.id = currentProfile.getId();
            mUser.name = currentProfile.getName();
            if (currentProfile != null) {
                displayProfileInfo(currentProfile);
            } else {
                // Fetch the profile, which will trigger the onCurrentProfileChanged receiver
                Profile.fetchProfileForCurrentAccessToken();
            }
        } else {
            // Otherwise, get Account Kit login information
            AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
                @Override
                public void onSuccess(final Account account) {
                    // get Account Kit ID
                    mUser.id = account.getId();
                    PhoneNumber phoneNumber = account.getPhoneNumber();
                    if (account.getPhoneNumber() != null) {
                        // if the phone number is available, display it
                        String formattedPhoneNumber = formatPhoneNumber(phoneNumber.toString());
                        mUser.name = formattedPhoneNumber;
                    } else {
                        // if the email address is available, display it
                        String emailString = account.getEmail();
                        mUser.name = emailString;
                    }

                }

                @Override
                public void onError(final AccountKitError error) {
                    String toastMessage = error.getErrorType().getMessage();
                    Toast.makeText(AccountActivity.this, toastMessage, Toast.LENGTH_LONG).show();
                }
            });
        }
    }


    public void onLogout(View view) {
        // logout of Account Kit
        AccountKit.logOut();
        // logout of Login Button
        LoginManager.getInstance().logOut();

        launchLoginActivity();
    }

    private void displayProfileInfo(Profile profile) {
        String profileId = profile.getId();
        String name = profile.getName();
        Uri profilePicUri = profile.getProfilePictureUri(100, 100);
        displayProfilePic(profilePicUri);
    }

    ///////////////Account functions///////////////////////////


    private void launchLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }


    ///////////////Util functions///////////////////////////

    private String formatPhoneNumber(String phoneNumber) {
        // helper method to format the phone number for display
        try {
            PhoneNumberUtil pnu = PhoneNumberUtil.getInstance();
            Phonenumber.PhoneNumber pn = pnu.parse(phoneNumber, Locale.getDefault().getCountry());
            phoneNumber = pnu.format(pn, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
        } catch (NumberParseException e) {
            e.printStackTrace();
        }
        return phoneNumber;
    }

    private void displayProfilePic(Uri uri) {
        // helper method to load the profile pic in a circular imageview
        Transformation transformation = new RoundedTransformationBuilder()
                .cornerRadiusDp(30)
                .oval(false)
                .build();
        Picasso.with(AccountActivity.this)
                .load(uri)
                .transform(transformation)
                .into(profilePic);
    }

    ///////////////Util functions///////////////////////////


    ///////////////Callback functions///////////////////////////

    @SuppressWarnings("MissingPermission")
    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;

        if (mLastLocation != null) {
            moveCamera(mLastLocation);
        }

        if (!checkPermissions()) {
            requestPermissions();
        } else {
            googleMap.setMyLocationEnabled(true);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_LOCATION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    try {
                        getAddress();
                    } catch (SecurityException sex) {
                        sex.printStackTrace();
                    }
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    ///////////////Callback functions///////////////////////////


    private class AddressResultReceiver extends ResultReceiver {
        AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string or an error message sent from the intent service.
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            displayAddressOutput();
        }
    }


    @SuppressWarnings("MissingPermission")
    private void getAddress() {
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location == null) {
                            Log.w(TAG, "onSuccess:null");
                            return;
                        }

                        mLastLocation = location;

                        // Determine whether a Geocoder is available.
                        if (!Geocoder.isPresent()) {
                            showSnackbar(getString(R.string.no_geocoder_available));
                            return;
                        }

                        startIntentService();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "getLastLocation:onFailure", e);
                    }
                });
    }


    ///////////////Helper functions///////////////////////////

    private void showSnackbar(final String text) {
        View container = findViewById(android.R.id.content);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }

    private void startIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);

        startService(intent);
    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void displayAddressOutput() {
        mAutocompleteFragmentSrc.setText(mAddressOutput);
        moveCamera(mLastLocation);
    }

    ///////////////Helper functions///////////////////////////


    public void onPick(View v) {

        if (mDestLatLng == null) {
            showSnackbar("Please enter the destination");
            return;
        }
        mUser.startTime = new Date().getTime();
        writeNewRoute(mUser);
    }

    private void writeNewUser(User user) {
        mFirebaseDbRef
                .child("routes")
                .child(user.type)
                .child(user.id)
                .setValue(user);
    }

    private void writeNewRoute(User user) {
        String key = mFirebaseDbRef.child("routes").child(user.type).push().getKey();

        Map<String, Object> childUpdates = new HashMap<>();
        Map<String, Object> userValues = user.toMap();


        childUpdates.put("/routes/" + user.type + "/" + key, userValues);

        mFirebaseDbRef.updateChildren(childUpdates);
    }

}
