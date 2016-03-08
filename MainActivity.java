package com.jonas.chormecaste2000;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

public class MainActivity extends AppCompatActivity {


    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private android.support.v7.media.MediaRouter.Callback mMediaRouterCallback;
    private CastDevice mSelectedDevice;
    private GoogleApiClient mApiClient;
    private Cast.Listener mCastClientListener;
    private final String TAG ="TEST";
    private Button mButton;
    private RemoteMediaPlayer mRemoteMediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMediaRouterCallback = new MyMediaRouterCallback(this);
        mButton = (Button) findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
                mediaMetadata.putString(MediaMetadata.KEY_TITLE, "COOOOOOL");
                MediaInfo mediaInfo = new MediaInfo.Builder(
                        "http://download.blender.org/peach/bigbuckbunny_movies/BigBuckBunny_320x180.mp4")
                        .setContentType("video/mp4")
                        .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                        .setMetadata(mediaMetadata)
                        .build();
                try {
                    mRemoteMediaPlayer.load(mApiClient, mediaInfo, true)
                            .setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                                @Override
                                public void onResult(RemoteMediaPlayer.MediaChannelResult result) {
                                    if (result.getStatus().isSuccess()) {
                                        Log.d(TAG, "Media loaded successfully");
                                    }
                                }
                            });
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Problem occurred with media during loading", e);
                } catch (Exception e) {
                    Log.e(TAG, "Problem opening media during loading", e);
                }
            }
        });
        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
       mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(CastMediaControlIntent.categoryForCast("4F8B3483"))
                .build();

        mCastClientListener = new Cast.Listener() {
            @Override
            public void onApplicationStatusChanged() {
                if (mApiClient != null) {
                    Log.d(TAG, "onApplicationStatusChanged: "
                            + Cast.CastApi.getApplicationStatus(mApiClient));
                }
            }

            @Override
            public void onVolumeChanged() {
                if (mApiClient != null) {
                    Log.d(TAG, "onVolumeChanged: " + Cast.CastApi.getVolume(mApiClient));
                }
            }

            @Override
            public void onApplicationDisconnected(int errorCode) {

            }
        };
        mRemoteMediaPlayer = new RemoteMediaPlayer();
        mRemoteMediaPlayer.setOnStatusUpdatedListener(
                new RemoteMediaPlayer.OnStatusUpdatedListener() {
                    @Override
                    public void onStatusUpdated() {
                        MediaStatus mediaStatus = mRemoteMediaPlayer.getMediaStatus();
                        boolean isPlaying = mediaStatus.getPlayerState() ==
                                MediaStatus.PLAYER_STATE_PLAYING;

                    }
                });

        mRemoteMediaPlayer.setOnMetadataUpdatedListener(
                new RemoteMediaPlayer.OnMetadataUpdatedListener() {
                    @Override
                    public void onMetadataUpdated() {
                        MediaInfo mediaInfo = mRemoteMediaPlayer.getMediaInfo();
                        MediaMetadata metadata = mediaInfo.getMetadata();
                    }
                });


    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.mainmenusxml, menu);
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
        return true;

    }

    @Override
    protected void onStart() {
        super.onStart();
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
    }

    @Override
    protected void onStop() {
        mMediaRouter.removeCallback(mMediaRouterCallback);
        super.onStop();
    }
    private class MyMediaRouterCallback extends MediaRouter.Callback {
        private Context mContext;

        public MyMediaRouterCallback(Context mContext) {
            this.mContext = mContext;
        }

        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
            mSelectedDevice = CastDevice.getFromBundle(info.getExtras());
            String routeId = info.getId();

            Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
                    .builder(mSelectedDevice, mCastClientListener);

            mApiClient = new GoogleApiClient.Builder(mContext)
                    .addApi(Cast.API, apiOptionsBuilder.build())
                    .addConnectionCallbacks( new ConnectionCallbacks())
                    .addOnConnectionFailedListener(new ConnectionFailedListener())
                    .build();

            mApiClient.connect();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
           // teardown();
            //mSelectedDevice = null;
        }
    }


    private class ConnectionCallbacks implements
            GoogleApiClient.ConnectionCallbacks {
        private boolean mWaitingForReconnect;
        @Override
        public void onConnected(Bundle connectionHint) {
            if (mWaitingForReconnect) {
                mWaitingForReconnect = false;
                //reconnectChannels();
            } else {
                try {
                    Cast.CastApi.launchApplication(mApiClient, "4F8B3483", false)
                            .setResultCallback(
                                    new ResultCallback<Cast.ApplicationConnectionResult>() {
                                        @Override
                                        public void onResult(Cast.ApplicationConnectionResult result) {
                                            Status status = result.getStatus();
                                            if (status.isSuccess()) {
                                                ApplicationMetadata applicationMetadata =
                                                        result.getApplicationMetadata();
                                                String sessionId = result.getSessionId();
                                                String applicationStatus = result.getApplicationStatus();
                                                boolean wasLaunched = result.getWasLaunched();

                                            } else {

                                            }
                                        }
                                    });

                } catch (Exception e) {
                    Log.e(TAG, "Failed to launch application", e);
                }
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
             mWaitingForReconnect = true;
        }
    }

    private class ConnectionFailedListener implements
            GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(ConnectionResult result) {

        }
    }


}


