package com.newtube.mobile.ui.common;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Monitors network connectivity changes and alerts UI listeners on the main thread,
 * matching official YouTube app online/offline behavior.
 */
public final class NetworkStatusMonitor {
    public interface Listener {
        void onNetworkStatusChanged(boolean isOnline);
    }

    private static volatile NetworkStatusMonitor sInstance;

    private final Context mAppContext;
    private final ConnectivityManager mConnectivityManager;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final CopyOnWriteArrayList<Listener> mListeners = new CopyOnWriteArrayList<>();

    private boolean mIsOnline;
    private boolean mHasInitialState;
    private ConnectivityManager.NetworkCallback mNetworkCallback;

    public static NetworkStatusMonitor instance(Context context) {
        if (sInstance == null) {
            synchronized (NetworkStatusMonitor.class) {
                if (sInstance == null) {
                    sInstance = new NetworkStatusMonitor(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    private NetworkStatusMonitor(Context appContext) {
        mAppContext = appContext;
        mConnectivityManager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mIsOnline = checkCurrentConnectivity();
        mHasInitialState = true;
        startMonitoring();
    }

    public boolean isOnline() {
        return checkCurrentConnectivity();
    }

    public void addListener(Listener listener) {
        if (listener == null) return;
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
        // Immediately notify with current state
        final boolean current = mIsOnline;
        mMainHandler.post(() -> listener.onNetworkStatusChanged(current));
    }

    public void removeListener(Listener listener) {
        if (listener != null) {
            mListeners.remove(listener);
        }
    }

    private boolean checkCurrentConnectivity() {
        if (mConnectivityManager == null) {
            return true;
        }
        try {
            Network activeNetwork = mConnectivityManager.getActiveNetwork();
            if (activeNetwork == null) {
                return false;
            }
            NetworkCapabilities caps = mConnectivityManager.getNetworkCapabilities(activeNetwork);
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (Exception e) {
            return true;
        }
    }

    private void startMonitoring() {
        if (mConnectivityManager == null) return;
        try {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();

            mNetworkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    dispatchChange(true);
                }

                @Override
                public void onLost(@NonNull Network network) {
                    // Double check if there's any other active internet network
                    mMainHandler.postDelayed(() -> dispatchChange(checkCurrentConnectivity()), 400);
                }

                @Override
                public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities caps) {
                    boolean hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                    dispatchChange(hasInternet);
                }
            };

            mConnectivityManager.registerNetworkCallback(request, mNetworkCallback);
        } catch (Exception ignored) {
        }
    }

    private void dispatchChange(boolean isOnline) {
        if (mHasInitialState && mIsOnline == isOnline) {
            return;
        }
        mIsOnline = isOnline;
        mHasInitialState = true;
        mMainHandler.post(() -> {
            for (Listener listener : mListeners) {
                try {
                    listener.onNetworkStatusChanged(isOnline);
                } catch (Exception ignored) {
                }
            }
        });
    }
}
